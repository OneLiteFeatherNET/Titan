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
import java.util.UUID;
import net.minestom.server.item.ItemStack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CarryRegistryTest {

    private static final Instant NOW = Instant.parse("2026-10-24T12:00:00Z");

    @DisplayName("A started carry is found by its thrower and by its target")
    @Test
    void startedCarryIsFoundByBothSides() {
        CarryRegistry registry = new CarryRegistry();
        Carry carry = carry(UUID.randomUUID(), UUID.randomUUID());

        registry.start(carry);

        Assertions.assertEquals(carry, registry.carryOf(carry.thrower()).orElseThrow(), "the thrower must find the carry");
        Assertions.assertEquals(carry, registry.carryingTarget(carry.target()).orElseThrow(), "the target must find the carry");
    }

    @DisplayName("A thrower cannot start a second carry while one runs")
    @Test
    void throwerCarriesAtMostOne() {
        CarryRegistry registry = new CarryRegistry();
        UUID thrower = UUID.randomUUID();
        registry.start(carry(thrower, UUID.randomUUID()));

        Assertions.assertThrows(IllegalStateException.class, () -> registry.start(carry(thrower, UUID.randomUUID())), "a second target for the same thrower must be rejected");
    }

    @DisplayName("A target cannot be carried twice at once")
    @Test
    void targetIsCarriedAtMostOnce() {
        CarryRegistry registry = new CarryRegistry();
        UUID target = UUID.randomUUID();
        registry.start(carry(UUID.randomUUID(), target));

        Assertions.assertThrows(IllegalStateException.class, () -> registry.start(carry(UUID.randomUUID(), target)), "a target already carried by someone else must be rejected");
    }

    @DisplayName("Ending a carry by the thrower frees both sides")
    @Test
    void endingByThrowerFreesBothSides() {
        CarryRegistry registry = new CarryRegistry();
        Carry carry = carry(UUID.randomUUID(), UUID.randomUUID());
        registry.start(carry);

        Assertions.assertEquals(carry, registry.endByThrower(carry.thrower()).orElseThrow(), "ending must hand back the carry, including its snapshot");
        Assertions.assertTrue(registry.carryOf(carry.thrower()).isEmpty(), "the thrower must be free after the end");
        Assertions.assertFalse(registry.isCarried(carry.target()), "the target must be free after the end");
    }

    @DisplayName("Ending a carry by the target frees both sides")
    @Test
    void endingByTargetFreesBothSides() {
        CarryRegistry registry = new CarryRegistry();
        Carry carry = carry(UUID.randomUUID(), UUID.randomUUID());
        registry.start(carry);

        registry.endByTarget(carry.target());

        Assertions.assertFalse(registry.isCarrying(carry.thrower()), "the thrower must be free after the target left");
    }

    @DisplayName("Charging is recorded per carry and only for a running carry")
    @Test
    void chargingIsRecordedForRunningCarry() {
        CarryRegistry registry = new CarryRegistry();
        Carry carry = carry(UUID.randomUUID(), UUID.randomUUID());
        registry.start(carry);

        registry.setCharging(carry.thrower(), true);

        Assertions.assertTrue(registry.carryOf(carry.thrower()).orElseThrow().charging(), "the carry must record that the charge runs");
        Assertions.assertTrue(registry.setCharging(UUID.randomUUID(), true).isEmpty(), "a player without a carry cannot start a charge");
    }

    @DisplayName("A carry expires after fifteen seconds")
    @Test
    void carryExpiresAfterFifteenSeconds() {
        Carry carry = carry(UUID.randomUUID(), UUID.randomUUID());

        Assertions.assertFalse(carry.expired(NOW.plusSeconds(14), Duration.ofSeconds(15)), "14 seconds into the carry it must still run");
        Assertions.assertTrue(carry.expired(NOW.plusSeconds(15), Duration.ofSeconds(15)), "15 seconds into the carry it must end");
    }

    private static Carry carry(UUID thrower, UUID target) {
        return new Carry(thrower, target, 4, ItemStack.AIR, NOW, false);
    }
}
