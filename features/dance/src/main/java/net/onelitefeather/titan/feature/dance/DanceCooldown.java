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
package net.onelitefeather.titan.feature.dance;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Keeps each player's dances apart by a fixed pause, read from the injected clock. */
final class DanceCooldown {

    /** The pause between two starts of the same player. */
    static final Duration PAUSE = Duration.ofSeconds(3);

    private final Clock clock;
    private final Map<UUID, Instant> lastStarts = new HashMap<>();

    DanceCooldown(Clock clock) {
        this.clock = clock;
    }

    /** @return {@code true} if {@code playerId} may start a dance now */
    boolean ready(UUID playerId) {
        Instant last = this.lastStarts.get(playerId);
        return last == null || !this.clock.instant().isBefore(last.plus(PAUSE));
    }

    /** Records that {@code playerId} has just started a dance. */
    void markStarted(UUID playerId) {
        this.lastStarts.put(playerId, this.clock.instant());
    }

    /** Drops the record of {@code playerId}, e.g. when the player disconnects. */
    void forget(UUID playerId) {
        this.lastStarts.remove(playerId);
    }
}
