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

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import net.onelitefeather.titan.core.testfixtures.AdjustableClock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClickCooldownTest {

    private static final Duration COOLDOWN = Duration.ofSeconds(2);

    @DisplayName("A second click within the cooldown is refused")
    @Test
    void secondClickWithinCooldownIsRefused() {
        AdjustableClock clock = new AdjustableClock(Instant.parse("2026-10-24T20:00:00Z"));
        ClickCooldown cooldown = new ClickCooldown(clock, COOLDOWN);
        UUID player = UUID.randomUUID();
        Assertions.assertTrue(cooldown.tryAcquire(player), "the first click is allowed");
        clock.advance(Duration.ofMillis(1500));
        Assertions.assertFalse(cooldown.tryAcquire(player), "a click 1.5 s later is still inside the 2 s cooldown");
    }

    @DisplayName("A click after the cooldown is allowed again")
    @Test
    void clickAfterCooldownIsAllowed() {
        AdjustableClock clock = new AdjustableClock(Instant.parse("2026-10-24T20:00:00Z"));
        ClickCooldown cooldown = new ClickCooldown(clock, COOLDOWN);
        UUID player = UUID.randomUUID();
        cooldown.tryAcquire(player);
        clock.advance(COOLDOWN);
        Assertions.assertTrue(cooldown.tryAcquire(player), "exactly the cooldown later the player may click again");
    }

    @DisplayName("One player's cooldown does not hold back another player")
    @Test
    void cooldownIsPerPlayer() {
        ClickCooldown cooldown = new ClickCooldown(new AdjustableClock(Instant.parse("2026-10-24T20:00:00Z")), COOLDOWN);
        cooldown.tryAcquire(UUID.randomUUID());
        Assertions.assertTrue(cooldown.tryAcquire(UUID.randomUUID()), "another player clicks freely");
    }
}
