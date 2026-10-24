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
import net.minestom.server.entity.EntityType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The throw's reach on flat ground, from the same flight model the client runs. Pure: no server.
 */
class ThrowRangeTest {

    private static final double MIN_CHARGE_BLOCKS = 6.0;
    private static final double FULL_CHARGE_BLOCKS = 20.0;

    @DisplayName("The flight model matches the air resistance the server uses for players")
    @Test
    void flightModelMatchesThePlayerPhysics() {
        Assertions.assertEquals(ThrowTrajectory.GRAVITY, EntityType.PLAYER.acceleration(), 1e-9, "gravity must be the player's gravity");
        Assertions.assertEquals(ThrowTrajectory.DRAG_HORIZONTAL, EntityType.PLAYER.horizontalAirResistance(), 1e-9, "horizontal drag must be the player's air resistance");
        Assertions.assertEquals(ThrowTrajectory.DRAG_VERTICAL, EntityType.PLAYER.verticalAirResistance(), 1e-9, "vertical drag must be the player's air resistance");
    }

    @DisplayName("A minimum charge at 45 degrees lands about six blocks away")
    @Test
    void minimumChargeLandsNearSixBlocks() {
        double range = rangeAt(Charge.strength(0), 45);

        Assertions.assertEquals(MIN_CHARGE_BLOCKS, range, 0.5, "the minimum charge must reach about six blocks, reached " + range);
    }

    @DisplayName("A full charge at 45 degrees lands about twenty blocks away")
    @Test
    void fullChargeLandsNearTwentyBlocks() {
        double range = rangeAt(Charge.strength(Charge.FULL_TICKS), 45);

        Assertions.assertEquals(FULL_CHARGE_BLOCKS, range, 1.0, "the full charge must reach about twenty blocks, reached " + range);
    }

    @DisplayName("A full charge at 30 degrees lands about twenty blocks away as well")
    @Test
    void fullChargeAtThirtyDegreesLandsNearTwentyBlocks() {
        double range = rangeAt(Charge.strength(Charge.FULL_TICKS), 30);

        Assertions.assertEquals(FULL_CHARGE_BLOCKS, range, 1.0, "the vertical cap makes 30 and 45 degrees land alike, reached " + range);
    }

    @DisplayName("A longer charge always reaches at least as far")
    @Test
    void rangeGrowsWithTheCharge() {
        double previous = 0.0;
        for (long ticks = 2; ticks <= Charge.FULL_TICKS; ticks += 2) {
            double range = rangeAt(Charge.strength(ticks), 45);
            Assertions.assertTrue(range >= previous, "a charge of " + ticks + " ticks reached " + range + ", less than the shorter charge's " + previous);
            previous = range;
        }
    }

    /**
     * The reach of a throw along a look at {@code pitchDegrees} above the horizon at
     * {@code strength}.
     */
    private static double rangeAt(double strength, double pitchDegrees) {
        double pitch = Math.toRadians(pitchDegrees);
        Vec look = new Vec(Math.cos(pitch), Math.sin(pitch), 0);
        return ThrowTrajectory.rangeOnFlatGround(ThrowVelocity.impulse(look, strength));
    }
}
