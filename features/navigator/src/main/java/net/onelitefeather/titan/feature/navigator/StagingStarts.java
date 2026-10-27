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
package net.onelitefeather.titan.feature.navigator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.onelitefeather.titan.core.service.OnDemandServices;
import net.onelitefeather.titan.core.service.ServiceState;
import net.onelitefeather.titan.core.service.ServiceState.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decides what a click on a staging entry does: connect at once when a service is ready, start one
 * when no service is alive, or make the player wait. The waiting players of an entry are connected
 * by the refresh once a service is ready, and refused once the configured timeout has passed. The
 * services are read
 * off the tick by the caller, so every method here runs on the tick thread with the reading it was
 * given and never blocks.
 *
 * @param <P> the player handle
 */
final class StagingStarts<P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingStarts.class);

    /** How long after its own start an entry waits before it may start again. */
    static final Duration COOLDOWN = Duration.ofSeconds(30);

    /** What a click led to. */
    enum Outcome {
        CONNECTED, WAITING, DENIED, REFUSED
    }

    private final OnDemandServices services;
    private final Clock clock;
    private final Duration timeout;
    private final StagingConnect<P> connect;
    private final StagingNotice<P> notice;
    private final NavigatorTelemetry telemetry;
    private final Map<Destination, Set<P>> waiting = new EnumMap<>(Destination.class);
    private final Map<Destination, Instant> waitingSince = new EnumMap<>(Destination.class);
    private final Map<Destination, Instant> lastStart = new EnumMap<>(Destination.class);

    StagingStarts(OnDemandServices services, Clock clock, Duration timeout, StagingConnect<P> connect, StagingNotice<P> notice, NavigatorTelemetry telemetry) {
        this.services = services;
        this.clock = clock;
        this.timeout = timeout;
        this.connect = connect;
        this.notice = notice;
        this.telemetry = telemetry;
    }

    /**
     * The services of the entry's task, or empty when they cannot be read. A provider failure
     * counts
     * as unreadable, never as a task that runs nowhere.
     */
    static Optional<List<ServiceState>> read(OnDemandServices services, Destination destination) {
        try {
            return services.services(destination.task());
        } catch (RuntimeException e) {
            LOGGER.debug("Reading the services of task '{}' failed", destination.task(), e);
            return Optional.empty();
        }
    }

    /**
     * The decision for one click, given the reading taken for it; a refused read starts nothing.
     */
    Outcome click(P player, Destination destination, Optional<List<ServiceState>> read) {
        if (!this.connect.allowed(player, destination)) {
            return Outcome.DENIED;
        }
        if (read.isEmpty()) {
            this.notice.unavailable(player);
            return Outcome.REFUSED;
        }
        List<ServiceState> services = read.get();
        if (services.stream().anyMatch(ServiceState::ready)) {
            this.unwait(player, destination);
            this.connect.connect(player, destination);
            return Outcome.CONNECTED;
        }
        Instant now = this.clock.instant();
        if (!services.stream().allMatch(service -> service.phase() == Phase.STOPPED)) {
            this.telemetry.stagingStart(destination, NavigatorTelemetry.StartResult.SKIPPED_STARTING);
        } else if (this.coolingDown(destination, now)) {
            this.telemetry.stagingStart(destination, NavigatorTelemetry.StartResult.COOLDOWN);
        } else if (!this.services.start(destination.task())) {
            this.telemetry.stagingStart(destination, NavigatorTelemetry.StartResult.FAILED);
            this.notice.unavailable(player);
            return Outcome.REFUSED;
        } else {
            this.lastStart.put(destination, now);
            this.telemetry.stagingStart(destination, NavigatorTelemetry.StartResult.REQUESTED);
            LOGGER.info("Requested the start of a service of task {}", destination.task());
        }
        this.enqueue(player, destination, now);
        return Outcome.WAITING;
    }

    /**
     * One pass after a refresh: connects the waiting players of every entry whose service is ready,
     * refuses those past the timeout, and repeats the action bar line for the rest.
     */
    void refresh(Map<Destination, Optional<List<ServiceState>>> reads) {
        Instant now = this.clock.instant();
        for (Destination destination : List.copyOf(this.waiting.keySet())) {
            this.refreshEntry(destination, reads.getOrDefault(destination, Optional.empty()), now);
        }
    }

    /** Removes a player who left the server from every waiting list. */
    void disconnected(P player) {
        Instant now = this.clock.instant();
        for (Destination destination : List.copyOf(this.waiting.keySet())) {
            Set<P> players = this.waiting.get(destination);
            if (players != null && players.contains(player)) {
                this.recordWait(destination, NavigatorTelemetry.WaitResult.LEFT, now);
            }
            this.unwait(player, destination);
        }
    }

    private void refreshEntry(Destination destination, Optional<List<ServiceState>> read, Instant now) {
        Set<P> players = this.waiting.get(destination);
        players.removeIf(player -> !this.connect.allowed(player, destination));
        if (players.isEmpty()) {
            this.clear(destination);
            return;
        }
        if (read.isPresent() && read.get().stream().anyMatch(ServiceState::ready)) {
            List<P> ready = List.copyOf(players);
            this.recordWaits(destination, NavigatorTelemetry.WaitResult.READY, now, players);
            this.clear(destination);
            ready.forEach(player -> this.connect.connect(player, destination));
            return;
        }
        if (!now.isBefore(this.waitingSince.get(destination).plus(this.timeout))) {
            LOGGER.warn("A service of task {} was not ready within {} seconds", destination.task(), this.timeout.toSeconds());
            List<P> expired = List.copyOf(players);
            this.recordWaits(destination, NavigatorTelemetry.WaitResult.TIMEOUT, now, players);
            this.clear(destination);
            expired.forEach(player -> this.notice.timeout(player, destination));
            return;
        }
        players.forEach(player -> this.notice.waiting(player, destination));
    }

    private boolean coolingDown(Destination destination, Instant now) {
        Instant started = this.lastStart.get(destination);
        return started != null && now.isBefore(started.plus(COOLDOWN));
    }

    private void enqueue(P player, Destination destination, Instant now) {
        Set<P> players = this.waiting.computeIfAbsent(destination, key -> new LinkedHashSet<>());
        if (players.isEmpty()) {
            this.waitingSince.put(destination, now);
        }
        players.add(player);
        this.notice.waiting(player, destination);
    }

    private void unwait(P player, Destination destination) {
        Set<P> players = this.waiting.get(destination);
        if (players == null) {
            return;
        }
        players.remove(player);
        if (players.isEmpty()) {
            this.clear(destination);
        }
    }

    private void clear(Destination destination) {
        this.waiting.remove(destination);
        this.waitingSince.remove(destination);
    }

    // The wait is measured from the entry's first waiter, the same point the timeout counts from.
    private void recordWaits(Destination destination, NavigatorTelemetry.WaitResult result, Instant now, Set<P> players) {
        for (P ignored : players) {
            this.recordWait(destination, result, now);
        }
    }

    private void recordWait(Destination destination, NavigatorTelemetry.WaitResult result, Instant now) {
        this.telemetry.stagingWait(destination, result, Duration.between(this.waitingSince.get(destination), now));
    }
}
