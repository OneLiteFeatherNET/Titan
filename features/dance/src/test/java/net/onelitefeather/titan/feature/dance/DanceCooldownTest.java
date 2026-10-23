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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DanceCooldownTest {

    private static final UUID PLAYER = new UUID(1, 2);

    private final AdjustableClock clock = new AdjustableClock(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC);

    @DisplayName("A player who has never danced may start at once")
    @Test
    void firstStartIsReady() {
        Assertions.assertTrue(new DanceCooldown(this.clock).ready(PLAYER), "a player without a previous start is ready");
    }

    @DisplayName("A player may not start again within the pause")
    @Test
    void startWithinThePauseIsNotReady() {
        DanceCooldown cooldown = new DanceCooldown(this.clock);
        cooldown.markStarted(PLAYER);
        this.clock.advance(Duration.ofSeconds(2));

        Assertions.assertFalse(cooldown.ready(PLAYER), "two seconds after a start the pause of three seconds still holds");
    }

    @DisplayName("A player may start again once the pause is over")
    @Test
    void startAfterThePauseIsReady() {
        DanceCooldown cooldown = new DanceCooldown(this.clock);
        cooldown.markStarted(PLAYER);
        this.clock.advance(DanceCooldown.PAUSE);

        Assertions.assertTrue(cooldown.ready(PLAYER), "exactly the pause after a start the player is ready again");
    }

    @DisplayName("Forgetting a player frees him at once")
    @Test
    void forgettingAPlayerFreesHim() {
        DanceCooldown cooldown = new DanceCooldown(this.clock);
        cooldown.markStarted(PLAYER);

        cooldown.forget(PLAYER);

        Assertions.assertTrue(cooldown.ready(PLAYER), "a player who left must not carry a pause into his next session");
    }

    @DisplayName("The pause is three seconds")
    @Test
    void pauseIsThreeSeconds() {
        Assertions.assertEquals(Duration.ofSeconds(3), DanceCooldown.PAUSE, "the pause between two starts");
    }
}
