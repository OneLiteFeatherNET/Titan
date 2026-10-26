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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.onelitefeather.titan.core.service.OnDemandServices;
import net.onelitefeather.titan.core.service.ServiceState;
import net.onelitefeather.titan.core.service.ServiceState.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stops a staging service that has stood without players for the configured idle time. Only the
 * entries marked {@link Destination#onDemand()} are ever stopped, so a live task is never touched.
 * The idle time is counted from the reads of the refresh cycle; a failed read freezes it, so the
 * time spent unreadable never counts. Before a stop, the service is read again, and only a service
 * that is still empty is stopped.
 */
final class StagingIdle {

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingIdle.class);

    /**
     * How long a service that was asked to stop is left alone, so the next cycles do not ask again.
     */
    static final Duration STOP_BLOCK = Duration.ofSeconds(60);

    private final OnDemandServices services;
    private final Clock clock;
    private final Duration limit;
    private final Map<Destination, Idleness> entries = new EnumMap<>(Destination.class);
    private final NavigatorTelemetry telemetry;

    StagingIdle(OnDemandServices services, Clock clock, Duration limit, NavigatorTelemetry telemetry) {
        this.services = services;
        this.clock = clock;
        this.limit = limit;
        this.telemetry = telemetry;
    }

    /**
     * One pass over the staging entries of a refresh cycle. Runs off the tick thread, where a read
     * or a stop may block; an unexpected failure of one entry leaves the others alone.
     */
    void refresh(Map<Destination, Optional<List<ServiceState>>> reads) {
        Instant now = this.clock.instant();
        for (Destination destination : Destination.values()) {
            if (!destination.onDemand()) {
                continue;
            }
            try {
                this.refreshEntry(destination, reads.getOrDefault(destination, Optional.empty()), now);
            } catch (RuntimeException e) {
                LOGGER.warn("Checking the idle staging services of task {} failed", destination.task(), e);
            }
        }
    }

    private void refreshEntry(Destination destination, Optional<List<ServiceState>> read, Instant now) {
        Idleness entry = this.entries.computeIfAbsent(destination, key -> new Idleness());
        if (read.isEmpty()) {
            entry.lastRead = null;
            return;
        }
        Duration elapsed = entry.lastRead == null ? Duration.ZERO : Duration.between(entry.lastRead, now);
        entry.lastRead = now;
        Map<String, Duration> idle = new HashMap<>();
        for (ServiceState service : read.get()) {
            if (!isEmpty(service)) {
                continue;
            }
            Duration before = entry.idle.get(service.name());
            Duration idleFor = before == null ? Duration.ZERO : before.plus(elapsed);
            idle.put(service.name(), idleFor);
            if (idleFor.compareTo(this.limit) >= 0 && !entry.blocked(service.name(), now)) {
                this.stopIfStillEmpty(destination, service.name(), entry, now);
            }
        }
        entry.idle.clear();
        entry.idle.putAll(idle);
    }

    // A fresh read right before the stop: a player who joined since the cycle's read keeps the service.
    private void stopIfStillEmpty(Destination destination, String name, Idleness entry, Instant now) {
        Optional<List<ServiceState>> fresh = StagingStarts.read(this.services, destination);
        if (fresh.isEmpty() || fresh.get().stream().noneMatch(service -> service.name().equals(name) && isEmpty(service))) {
            return;
        }
        entry.stopBlockedUntil.put(name, now.plus(STOP_BLOCK));
        if (this.request(name)) {
            this.telemetry.stagingStop(destination, NavigatorTelemetry.StopResult.REQUESTED);
            LOGGER.info("Requested the stop of idle staging service {} of task {}", name, destination.task());
        } else {
            this.telemetry.stagingStop(destination, NavigatorTelemetry.StopResult.FAILED);
            LOGGER.warn("The stop of idle staging service {} of task {} was refused", name, destination.task());
        }
    }

    private boolean request(String name) {
        try {
            return this.services.stop(name);
        } catch (RuntimeException e) {
            LOGGER.warn("Stopping staging service {} failed", name, e);
            return false;
        }
    }

    // Only a running service with nobody on it counts as idle; a starting one has not begun to be used yet.
    private static boolean isEmpty(ServiceState service) {
        return service.phase() == Phase.RUNNING && service.online() == 0;
    }

    /** The idle time per service of one entry, and when its last successful read was. */
    private static final class Idleness {

        private final Map<String, Duration> idle = new HashMap<>();
        private final Map<String, Instant> stopBlockedUntil = new HashMap<>();
        // Null after a failed read, so the next successful read adds no time for the failing period.
        private Instant lastRead;

        boolean blocked(String name, Instant now) {
            Instant until = this.stopBlockedUntil.get(name);
            return until != null && now.isBefore(until);
        }
    }
}
