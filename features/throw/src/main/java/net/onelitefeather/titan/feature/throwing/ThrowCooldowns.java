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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * The limits against harassment: a thrower waits after a throw, a target waits between pickups
 * whoever picks up, and a pair waits after the target escaped. Everything is in memory and reads
 * time from the injected clock.
 */
final class ThrowCooldowns {

    static final Duration THROWER_AFTER_THROW = Duration.ofSeconds(5);
    static final Duration TARGET_BETWEEN_PICKUPS = Duration.ofSeconds(20);
    static final Duration PAIR_AFTER_ESCAPE = Duration.ofSeconds(60);

    private final Clock clock;
    private final Map<UUID, Instant> lastThrow = new HashMap<>();
    private final Map<UUID, Instant> lastPickup = new HashMap<>();
    private final Map<Pair, Instant> lastEscape = new HashMap<>();

    ThrowCooldowns(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    boolean throwerMayPickUp(UUID thrower) {
        return elapsed(this.lastThrow.get(thrower), THROWER_AFTER_THROW);
    }

    void throwerThrew(UUID thrower) {
        this.lastThrow.put(thrower, this.clock.instant());
    }

    boolean targetMayBePickedUp(UUID target) {
        return elapsed(this.lastPickup.get(target), TARGET_BETWEEN_PICKUPS);
    }

    void targetPickedUp(UUID target) {
        this.lastPickup.put(target, this.clock.instant());
    }

    boolean pairMayPickUp(UUID thrower, UUID target) {
        return elapsed(this.lastEscape.get(new Pair(thrower, target)), PAIR_AFTER_ESCAPE);
    }

    void pairEscaped(UUID thrower, UUID target) {
        this.lastEscape.put(new Pair(thrower, target), this.clock.instant());
    }

    private boolean elapsed(Instant since, Duration cooldown) {
        return since == null || !this.clock.instant().isBefore(since.plus(cooldown));
    }

    private record Pair(UUID thrower, UUID target) {
    }
}
