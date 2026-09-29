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
package net.onelitefeather.titan.feature.season;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minestom.server.ServerFlag;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.core.module.FeatureNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compares the world the lobby started with to the world the calendar wants now, once a minute
 * and after every disconnect. On a mismatch a restart is pending; once the lobby is empty it
 * stops, and the supervisor starts it in the desired world. The world is never switched live.
 */
@Singleton
final class SeasonModule {

    // A tick count, not TaskSchedule.minutes(): a duration schedule runs on the wall clock, out of reach of env.tick().
    static final int CHECK_INTERVAL_TICKS = ServerFlag.SERVER_TICKS_PER_SECOND * 60;
    static final int EVENT_PRIORITY = 800;

    private static final Logger LOGGER = LoggerFactory.getLogger(SeasonModule.class);
    private static final String ID = "season";

    private final EventNode<Event> titan;
    private final Scheduler scheduler;
    private final Clock clock;
    private final SeasonSchedule schedule;
    private final StartedWorld startedWorld;
    private final OnlinePlayers onlinePlayers;
    private final ServerStop serverStop;
    private final RestartPolicy policy = new RestartPolicy();
    private final AtomicBoolean stopRequested = new AtomicBoolean();
    // Written and read on the tick thread only; volatile for the tests that read it from outside.
    private volatile Instant restartPendingSince;
    private Optional<String> started = Optional.empty();
    private FeatureNode node;
    private Task task;

    SeasonModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Scheduler scheduler, Clock clock, SeasonSchedule schedule, StartedWorld startedWorld, OnlinePlayers onlinePlayers, ServerStop serverStop) {
        this.titan = Objects.requireNonNull(titan, "titan");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.schedule = Objects.requireNonNull(schedule, "schedule");
        this.startedWorld = Objects.requireNonNull(startedWorld, "startedWorld");
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "onlinePlayers");
        this.serverStop = Objects.requireNonNull(serverStop, "serverStop");
    }

    @PostConstruct
    void start() {
        this.started = this.startedWorld.worldName();
        // Checked next tick, not inside the event, so the count no longer includes the leaving player.
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY).on(PlayerDisconnectEvent.class, event -> this.scheduler.scheduleNextTick(this::check));
        TaskSchedule interval = TaskSchedule.tick(CHECK_INTERVAL_TICKS);
        this.task = this.scheduler.scheduleTask(this::check, interval, interval);
    }

    @PreDestroy
    void stop() {
        this.task.cancel();
        this.node.close();
    }

    private void check() {
        switch (this.schedule.now()) {
            case SeasonSchedule.Desired.Chosen chosen -> apply(chosen.season());
            case SeasonSchedule.Desired.Unresolvable ignored -> {
                // Already warned about by the reader; keep the current state and do nothing.
            }
        }
    }

    private void apply(Optional<Season> desiredSeason) {
        RestartPolicy.Decision decision = this.policy.decide(this.started, desiredSeason.map(Season::world), this.onlinePlayers.count());
        switch (decision) {
            case NONE -> clearPending();
            case PENDING -> markPending(desiredSeason);
            case STOP -> {
                markPending(desiredSeason);
                requestStop();
            }
        }
    }

    private void markPending(Optional<Season> desiredSeason) {
        if (this.restartPendingSince != null) {
            return;
        }
        this.restartPendingSince = this.clock.instant();
        desiredSeason.ifPresentOrElse(season -> LOGGER.info("Restart for season {} pending since {}", season.id(), this.restartPendingSince), () -> LOGGER.info("Restart to the default world pending since {}", this.restartPendingSince));
    }

    private void clearPending() {
        if (this.restartPendingSince != null) {
            this.restartPendingSince = null;
            LOGGER.info("Restart no longer needed");
        }
    }

    private void requestStop() {
        // Both triggers can fire before the server is down; stop only once.
        if (this.stopRequested.compareAndSet(false, true)) {
            LOGGER.info("Stopping lobby for season change");
            this.serverStop.stop();
        }
    }
}
