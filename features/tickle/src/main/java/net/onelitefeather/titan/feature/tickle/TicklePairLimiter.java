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
package net.onelitefeather.titan.feature.tickle;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets one ordered pair (tickler, target) hear a tickle sound at most once per {@link #INTERVAL}.
 * Reads "now" from the injected {@link Clock}, so tests control the window.
 */
final class TicklePairLimiter {

    /** Fixed on purpose: a knob nobody needs would only be one more thing to mis-set. */
    static final Duration INTERVAL = Duration.ofSeconds(2);

    private record Pair(UUID tickler, UUID target) {
    }

    private final Clock clock;
    private final Map<Pair, Instant> lastPlayed = new ConcurrentHashMap<>();

    TicklePairLimiter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * @return {@code true} if the pair may hear a sound now; the sound then counts as played
     */
    boolean tryAcquire(UUID tickler, UUID target) {
        Instant now = this.clock.instant();
        // Entries older than the window can no longer block anything; dropping them keeps the map small.
        this.lastPlayed.values().removeIf(played -> !now.isBefore(played.plus(INTERVAL)));
        Pair pair = new Pair(tickler, target);
        if (this.lastPlayed.containsKey(pair)) {
            return false;
        }
        this.lastPlayed.put(pair, now);
        return true;
    }
}
