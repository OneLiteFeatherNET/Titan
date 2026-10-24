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
package net.onelitefeather.titan.feature.tag;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TagCooldownTest {

    private static final long START = 1_000_000L;
    private static final UUID ANNA = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BEN = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @DisplayName("The reverse hit is blocked within the cooldown")
    @Test
    void reverseHitIsBlockedWithinCooldown() {
        TagCooldown cooldown = new TagCooldown();
        cooldown.passed(ANNA, BEN, START);

        Assertions.assertTrue(cooldown.blocks(BEN, ANNA, START + TagGame.HIT_COOLDOWN_MILLIS - 1), "the player who was tagged must not tag back at once");
    }

    @DisplayName("The reverse hit is free again at exactly the end of the cooldown")
    @Test
    void reverseHitIsFreeAtTheEndOfTheCooldown() {
        TagCooldown cooldown = new TagCooldown();
        cooldown.passed(ANNA, BEN, START);

        Assertions.assertFalse(cooldown.blocks(BEN, ANNA, START + TagGame.HIT_COOLDOWN_MILLIS), "after five seconds the reverse hit must be allowed");
    }

    @DisplayName("Forgetting a player drops every pair that player is part of")
    @Test
    void forgettingAPlayerDropsTheirPairs() {
        TagCooldown cooldown = new TagCooldown();
        cooldown.passed(ANNA, BEN, START);

        cooldown.forget(ANNA);

        Assertions.assertFalse(cooldown.blocks(BEN, ANNA, START), "a player who left must not keep a cooldown on the others");
    }
}
