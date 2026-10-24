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
package net.onelitefeather.titan.feature.throwing;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks thrown players from the throw until they have landed for a moment, so the portal logic
 * does not hand a flying player to another server. A flight ends at the landing or after
 * {@link #MAX_FLIGHT}, whichever comes first; the landing keeps the protection for
 * {@link #LANDING_GRACE} more.
 */
final class FlightProtection {

    static final Duration MAX_FLIGHT = Duration.ofSeconds(5);
    static final Duration LANDING_GRACE = Duration.ofSeconds(2);

    private final Map<UUID, Flight> flights = new HashMap<>();

    void launch(UUID player, Instant now) {
        this.flights.put(player, new Flight(now, false, null));
    }

    /**
     * Called every tick with the player's ground state; a landing is leaving the air and touching
     * the ground again.
     */
    void observeGround(UUID player, boolean onGround, Instant now) {
        Flight flight = this.flights.get(player);
        if (flight == null || flight.landedAt() != null) {
            return;
        }
        if (!onGround) {
            this.flights.put(player, new Flight(flight.launchedAt(), true, null));
        } else if (flight.airborne()) {
            this.flights.put(player, new Flight(flight.launchedAt(), true, now));
        }
    }

    /** @return whether {@code player} is still in the air at {@code now} */
    boolean inFlight(UUID player, Instant now) {
        Flight flight = this.flights.get(player);
        if (flight == null) {
            return false;
        }
        Instant end = flight.launchedAt().plus(MAX_FLIGHT);
        if (flight.landedAt() != null && flight.landedAt().isBefore(end)) {
            end = flight.landedAt();
        }
        return now.isBefore(end);
    }

    /** @return whether a portal may hand {@code player} to another server at {@code now} */
    boolean mayTransfer(UUID player, Instant now) {
        if (inFlight(player, now)) {
            return false;
        }
        Flight flight = this.flights.get(player);
        return flight == null || flight.landedAt() == null || !now.isBefore(flight.landedAt().plus(LANDING_GRACE));
    }

    void forget(UUID player) {
        this.flights.remove(player);
    }

    /**
     * Drops the players whose protection has run out, so the map does not grow with every throw.
     */
    void prune(Instant now) {
        this.flights.keySet().removeIf(player -> mayTransfer(player, now));
    }

    private record Flight(Instant launchedAt, boolean airborne, Instant landedAt) {
    }
}
