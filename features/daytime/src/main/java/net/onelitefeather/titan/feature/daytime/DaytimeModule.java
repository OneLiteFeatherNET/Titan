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

    private final Instance lobby;
    private final Scheduler scheduler;
    private final Clock clock;
    private final DayTimeMapping mapping = new DayTimeMapping();
    private ZoneId zone;
    private String rejectedZone;
    private Task task;

    @Inject
    public DaytimeModule(Instance lobby, Scheduler scheduler, Clock clock) {
        this.lobby = Objects.requireNonNull(lobby, "lobby");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @PostConstruct
    void start() {
        // Aborts startup on an invalid zone; later changes fall back to this last valid one.
        this.zone = Config.getAs(DaytimeSettings.ZONE_KEY, DaytimeSettings::zone);
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
        long ticks = Config.getBool(DaytimeSettings.ENABLED_KEY) ? this.mapping.ticksAt(this.clock.instant(), currentZone()) : DayTimeMapping.NOON_TICKS;
        this.lobby.setTime(ticks);
    }

    private ZoneId currentZone() {
        String raw = Config.get(DaytimeSettings.ZONE_KEY);
        try {
            this.zone = DaytimeSettings.zone(raw);
            this.rejectedZone = null;
        } catch (DateTimeException e) {
            warnOnceAbout(raw, e);
        }
        return this.zone;
    }

    private void warnOnceAbout(String rejected, DateTimeException reason) {
        if (!rejected.equals(this.rejectedZone)) {
            this.rejectedZone = rejected;
            LOGGER.warn("Ignoring invalid {} '{}' ({}), keeping zone {}", DaytimeSettings.ZONE_KEY, rejected, reason.getMessage(), this.zone);
        }
    }
}
