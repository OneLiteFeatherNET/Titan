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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The pitch is drawn from a seeded generator, so the test is repeatable (F.I.R.S.T.). */
class TickleSoundsTest {

    @DisplayName("Every tickle pitch stays within the configured range")
    @Test
    void pitchStaysWithinRange() {
        TickleSounds sounds = new TickleSounds(new TicklePairLimiter(Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC)), new Random(42));

        for (int draw = 0; draw < 1_000; draw++) {
            float pitch = sounds.pitch();
            Assertions.assertTrue(pitch >= TickleSounds.MIN_PITCH && pitch < TickleSounds.MAX_PITCH, "pitch " + pitch + " outside [" + TickleSounds.MIN_PITCH + ", " + TickleSounds.MAX_PITCH + ")");
        }
    }
}
