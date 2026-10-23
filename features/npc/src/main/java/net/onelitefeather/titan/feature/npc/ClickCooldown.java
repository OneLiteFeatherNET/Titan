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
package net.onelitefeather.titan.feature.npc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * One click per player within the cooldown. The time comes from the injected clock, so the rule
 * is testable without waiting.
 */
final class ClickCooldown {

    private final Clock clock;
    private final Duration cooldown;
    private final Map<UUID, Instant> last = new HashMap<>();

    ClickCooldown(Clock clock, Duration cooldown) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.cooldown = Objects.requireNonNull(cooldown, "cooldown");
    }

    /** Whether the player may click now; a click that is allowed starts the cooldown. */
    boolean tryAcquire(UUID player) {
        Instant now = this.clock.instant();
        Instant previous = this.last.get(player);
        if (previous != null && now.isBefore(previous.plus(this.cooldown))) {
            return false;
        }
        this.last.put(player, now);
        return true;
    }

    /** Forgets a player who left, so the map does not grow with every visitor. */
    void forget(UUID player) {
        this.last.remove(player);
    }
}
