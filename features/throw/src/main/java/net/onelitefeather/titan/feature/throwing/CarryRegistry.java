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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The running carries of this lobby: one target per thrower, and one thrower per target. Tick
 * thread only.
 */
final class CarryRegistry {

    private final Map<UUID, Carry> byThrower = new HashMap<>();
    private final Map<UUID, UUID> throwerOfTarget = new HashMap<>();

    /**
     * @throws IllegalStateException if the thrower already carries someone or the target is carried
     */
    void start(Carry carry) {
        Objects.requireNonNull(carry, "carry must not be null");
        if (this.byThrower.containsKey(carry.thrower())) {
            throw new IllegalStateException("thrower " + carry.thrower() + " already carries a player");
        }
        if (this.throwerOfTarget.containsKey(carry.target())) {
            throw new IllegalStateException("target " + carry.target() + " is already carried");
        }
        this.byThrower.put(carry.thrower(), carry);
        this.throwerOfTarget.put(carry.target(), carry.thrower());
    }

    Optional<Carry> carryOf(UUID thrower) {
        return Optional.ofNullable(this.byThrower.get(thrower));
    }

    Optional<Carry> carryingTarget(UUID target) {
        UUID thrower = this.throwerOfTarget.get(target);
        return thrower == null ? Optional.empty() : carryOf(thrower);
    }

    boolean isCarrying(UUID thrower) {
        return this.byThrower.containsKey(thrower);
    }

    boolean isCarried(UUID target) {
        return this.throwerOfTarget.containsKey(target);
    }

    /** @return the carry that was running, now removed */
    Optional<Carry> endByThrower(UUID thrower) {
        Carry carry = this.byThrower.remove(thrower);
        if (carry != null) {
            this.throwerOfTarget.remove(carry.target());
        }
        return Optional.ofNullable(carry);
    }

    /** @return the carry that was running, now removed */
    Optional<Carry> endByTarget(UUID target) {
        UUID thrower = this.throwerOfTarget.remove(target);
        return thrower == null ? Optional.empty() : endByThrower(thrower);
    }

    /** @return the updated carry, or empty if the thrower carries nobody */
    Optional<Carry> setCharging(UUID thrower, boolean charging) {
        Carry carry = this.byThrower.get(thrower);
        if (carry == null) {
            return Optional.empty();
        }
        Carry updated = carry.withCharging(charging);
        this.byThrower.put(thrower, updated);
        return Optional.of(updated);
    }

    /** A copy, so the caller may end carries while iterating. */
    List<Carry> all() {
        return new ArrayList<>(this.byThrower.values());
    }
}
