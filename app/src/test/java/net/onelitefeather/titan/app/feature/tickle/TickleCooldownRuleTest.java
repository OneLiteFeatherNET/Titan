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
package net.onelitefeather.titan.app.feature.tickle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the pure {@link TickleCooldownRule}. Plain {@code long} inputs, no {@link
 * net.minestom.server.entity.Player} or clock needed - the fastest, most repeatable layer of the
 * test pyramid for this feature.
 */
class TickleCooldownRuleTest {

    @DisplayName("No cooldown tag yet: tickle")
    @Test
    void noTagTickles() {
        Assertions.assertEquals(TickleCooldownRule.Decision.TICKLE, TickleCooldownRule.decide(false, 0L, 1_000L));
    }

    @DisplayName("Tag present and not yet expired: still on cooldown")
    @Test
    void tagPresentAndNotExpiredIsOnCooldown() {
        Assertions.assertEquals(TickleCooldownRule.Decision.ON_COOLDOWN, TickleCooldownRule.decide(true, 2_000L, 1_000L));
    }

    @DisplayName("Tag present and exactly at its expiry: still on cooldown")
    @Test
    void tagPresentAndExactlyAtExpiryIsOnCooldown() {
        Assertions.assertEquals(TickleCooldownRule.Decision.ON_COOLDOWN, TickleCooldownRule.decide(true, 1_000L, 1_000L));
    }

    @DisplayName("Tag present and expired: today's code only clears it (known bug, kept on purpose)")
    @Test
    void tagPresentAndExpiredOnlyClears() {
        Assertions.assertEquals(TickleCooldownRule.Decision.CLEAR_EXPIRED_TAG, TickleCooldownRule.decide(true, 1_000L, 1_001L));
    }

    @DisplayName("expiryAfter adds the cooldown duration to now")
    @Test
    void expiryAfterAddsCooldownToNow() {
        Assertions.assertEquals(5_000L, TickleCooldownRule.expiryAfter(1_000L, 4_000L));
    }

    @DisplayName("expiryAfter with a zero cooldown returns now")
    @Test
    void expiryAfterWithZeroCooldownReturnsNow() {
        Assertions.assertEquals(1_000L, TickleCooldownRule.expiryAfter(1_000L, 0L));
    }
}
