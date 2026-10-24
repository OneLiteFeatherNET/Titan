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

import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ThrowVelocityTest {

    @DisplayName("Looking east at full strength throws east at 16 blocks per second")
    @Test
    void eastAtFullStrengthIsSixteenBlocksPerSecond() {
        Vec impulse = ThrowVelocity.impulse(new Vec(1, 0, 0), 1.0);

        Assertions.assertEquals(16.0, impulse.x(), 1e-9, "the impulse must point east at the full speed");
        Assertions.assertEquals(0.0, impulse.y(), 1e-9, "a level look must not lift the thrown player");
        Assertions.assertEquals(0.0, impulse.z(), 1e-9, "an east look must not drift sideways");
    }

    @DisplayName("The speed of the impulse equals the charged speed")
    @Test
    void magnitudeEqualsTheChargedSpeed() {
        Vec impulse = ThrowVelocity.impulse(new Vec(0.3, -0.2, 0.9), 0.5);

        Assertions.assertEquals(12.0, impulse.length(), 1e-6, "a half charge must throw at 12 blocks per second");
    }

    @DisplayName("The vertical part never exceeds ten blocks per second")
    @Test
    void verticalPartIsCapped() {
        Vec impulse = ThrowVelocity.impulse(new Vec(0, 1, 0), 1.0);

        Assertions.assertTrue(impulse.y() <= 10.0 + 1e-9, "the vertical part must stay at or below 10 blocks per second, was " + impulse.y());
    }

    @DisplayName("Looking steeply down keeps the vertical part non-negative")
    @Test
    void lookingDownDoesNotPushDownwards() {
        Vec impulse = ThrowVelocity.impulse(new Vec(0.1, -1, 0), 1.0);

        Assertions.assertTrue(impulse.y() >= 0.0, "a steep look down must not produce a downward impulse, was " + impulse.y());
    }

    @DisplayName("Looking steeply down keeps the magnitude at the charged speed")
    @Test
    void lookingDownKeepsTheMagnitude() {
        Vec impulse = ThrowVelocity.impulse(new Vec(0.1, -1, 0), 1.0);

        Assertions.assertEquals(16.0, impulse.length(), 1e-6, "the magnitude must not change when the look points down");
    }

    @DisplayName("Looking straight down throws straight up at a speed from 8 to 16 blocks per second")
    @Test
    void lookingStraightDownHasAMagnitudeWithinTheRange() {
        Vec impulse = ThrowVelocity.impulse(new Vec(0, -1, 0), 0.0);

        Assertions.assertTrue(impulse.length() >= 8.0 && impulse.length() <= 16.0, "the magnitude must stay within 8 to 16 blocks per second, was " + impulse.length());
        Assertions.assertEquals(0.0, impulse.x(), 1e-9, "a straight look has no horizontal part");
    }
}
