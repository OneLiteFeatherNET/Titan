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

import net.onelitefeather.titan.core.testfixtures.AdjustableClock;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ThrowCooldownsTest {

    private static final Instant NOW = Instant.parse("2026-10-24T12:00:00Z");

    @DisplayName("A thrower may pick up again only five seconds after a throw")
    @Test
    void throwerWaitsFiveSecondsAfterAThrow() {
        AdjustableClock clock = new AdjustableClock(NOW, ZoneOffset.UTC);
        ThrowCooldowns cooldowns = new ThrowCooldowns(clock);
        UUID thrower = UUID.randomUUID();

        cooldowns.throwerThrew(thrower);
        clock.advance(Duration.ofSeconds(2));
        Assertions.assertFalse(cooldowns.throwerMayPickUp(thrower), "two seconds after a throw the thrower must still wait");
        clock.advance(Duration.ofSeconds(3));
        Assertions.assertTrue(cooldowns.throwerMayPickUp(thrower), "five seconds after a throw the thrower may pick up again");
    }

    @DisplayName("A target may be picked up again only twenty seconds after the last pickup, whoever picks up")
    @Test
    void targetWaitsTwentySecondsAfterAPickup() {
        AdjustableClock clock = new AdjustableClock(NOW, ZoneOffset.UTC);
        ThrowCooldowns cooldowns = new ThrowCooldowns(clock);
        UUID target = UUID.randomUUID();

        cooldowns.targetPickedUp(target);
        clock.advance(Duration.ofSeconds(10));
        Assertions.assertFalse(cooldowns.targetMayBePickedUp(target), "ten seconds after a pickup the target must be protected");
        clock.advance(Duration.ofSeconds(10));
        Assertions.assertTrue(cooldowns.targetMayBePickedUp(target), "twenty seconds after a pickup the target may be picked up again");
    }

    @DisplayName("A pair waits sixty seconds after an escape")
    @Test
    void pairWaitsSixtySecondsAfterAnEscape() {
        AdjustableClock clock = new AdjustableClock(NOW, ZoneOffset.UTC);
        ThrowCooldowns cooldowns = new ThrowCooldowns(clock);
        UUID thrower = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        cooldowns.pairEscaped(thrower, target);
        clock.advance(Duration.ofSeconds(59));
        Assertions.assertFalse(cooldowns.pairMayPickUp(thrower, target), "59 seconds after an escape the pair must still wait");
        clock.advance(Duration.ofSeconds(1));
        Assertions.assertTrue(cooldowns.pairMayPickUp(thrower, target), "60 seconds after an escape the pair may try again");
    }

    @DisplayName("An escape blocks only the pair that escaped")
    @Test
    void escapeDoesNotBlockAnotherThrower() {
        AdjustableClock clock = new AdjustableClock(NOW, ZoneOffset.UTC);
        ThrowCooldowns cooldowns = new ThrowCooldowns(clock);
        UUID target = UUID.randomUUID();

        cooldowns.pairEscaped(UUID.randomUUID(), target);

        Assertions.assertTrue(cooldowns.pairMayPickUp(UUID.randomUUID(), target), "another thrower is not on the cooldown of this pair");
    }

    @DisplayName("A thrower with no history may pick up at once")
    @Test
    void unknownPlayersAreNotOnCooldown() {
        ThrowCooldowns cooldowns = new ThrowCooldowns(new AdjustableClock(NOW, ZoneOffset.UTC));

        Assertions.assertTrue(cooldowns.throwerMayPickUp(UUID.randomUUID()), "a player without a throw in the past is not on cooldown");
    }
}
