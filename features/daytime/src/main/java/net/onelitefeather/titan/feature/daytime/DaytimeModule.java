/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.feature.daytime;

import io.avaje.config.Config;
import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Objects;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets the lobby's time of day once per second from the wall clock of the configured zone, and
 * holds it at midday while {@code daytime.enabled} is {@code false}.
 */
@Singleton
public final class DaytimeModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaytimeModule.class);
    private static final TaskSchedule UPDATE_INTERVAL = TaskSchedule.tick(20);
    private static final AttributeKey<String> REASON = AttributeKey.stringKey("reason");
    private static final String REASON_BLANK = "blank";
    private static final String REASON_INVALID = "invalid";

    private final Instance lobby;
    private final Scheduler scheduler;
    private final Clock clock;
    private final LongCounter updates;
    private final LongCounter configRejected;
    private final DayTimeMapping mapping = new DayTimeMapping();
    // Written by start() and by the scheduler thread in update().
    private volatile ZoneId zone;
    private volatile String rejectedZone;
    private Task task;

    @Inject
    public DaytimeModule(Instance lobby, Scheduler scheduler, Clock clock, Telemetry telemetry) {
        this.lobby = Objects.requireNonNull(lobby, "lobby");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.updates = Objects.requireNonNull(telemetry, "telemetry").meter().counterBuilder("daytime.updates").setUnit("{update}").setDescription("Time-of-day updates of the lobby").build();
        this.configRejected = telemetry.meter().counterBuilder("daytime.config_rejected").setUnit("{rejection}").setDescription("Changed daytime.zone values that were rejected").build();
    }

    @PostConstruct
    void start() {
        // Aborts startup on an invalid zone; later changes fall back to this last valid one.
        this.zone = Config.getAs(DaytimeSettings.ZONE_KEY, ZoneId::of);
        freezeMinestomClock();
        // The first run sets the time now, then once per second; MapProvider's midday is only a fallback.
        update();
        this.task = this.scheduler.scheduleTask(this::update, UPDATE_INTERVAL, UPDATE_INTERVAL);
    }

    @PreDestroy
    void stop() {
        this.task.cancel();
    }

    private void freezeMinestomClock() {
        var minestomClock = this.lobby.defaultClock();
        if (minestomClock == null) {
            LOGGER.warn("The lobby instance has no default clock; its time may keep running between updates");
        } else {
            minestomClock.rate(0f);
        }
    }

    private void update() {
        this.updates.add(1);
        long ticks = enabled() ? this.mapping.ticksAt(this.clock.instant(), currentZone()) : DayTimeMapping.NOON_TICKS;
        this.lobby.setTime(ticks);
    }

    private static boolean enabled() {
        return Config.getBool(DaytimeSettings.ENABLED_KEY);
    }

    private ZoneId currentZone() {
        String raw = Config.get(DaytimeSettings.ZONE_KEY, this.zone.getId());
        if (raw.isBlank()) {
            rejectOnce(raw, "value is blank", REASON_BLANK);
            return this.zone;
        }
        try {
            this.zone = ZoneId.of(raw);
            this.rejectedZone = null;
        } catch (DateTimeException e) {
            rejectOnce(raw, e.getMessage(), REASON_INVALID);
        }
        return this.zone;
    }

    /**
     * Warns and counts a rejection once per changed value; the counter's reason stays a fixed word.
     */
    private void rejectOnce(String rejected, String reason, String counterReason) {
        if (!rejected.equals(this.rejectedZone)) {
            this.rejectedZone = rejected;
            this.configRejected.add(1, Attributes.of(REASON, counterReason));
            LOGGER.warn("Ignoring invalid {} '{}' ({}), keeping zone {}", DaytimeSettings.ZONE_KEY, rejected, reason, this.zone);
        }
    }
}
