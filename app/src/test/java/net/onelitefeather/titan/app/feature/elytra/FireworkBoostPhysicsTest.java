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
package net.onelitefeather.titan.app.feature.elytra;

import java.util.Random;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link FireworkBoostPhysics}: known inputs to expected vectors, and the
 * lifetime formula with a seeded {@link Random}. Pure math, no {@code Env} or server required -
 * F.I.R.S.T.
 */
class FireworkBoostPhysicsTest {

    private static final double TOLERANCE = 1.0E-9;

    @DisplayName("nextVelocity() reproduces the vanilla per-tick formula for a known input")
    @Test
    void nextVelocityReproducesTheVanillaFormulaForAKnownInput() {
        Vec velocity = new Vec(1.0, 0.0, 0.0);
        Vec look = new Vec(0.0, 0.0, 1.0);

        Vec next = FireworkBoostPhysics.nextVelocity(velocity, look);

        // vel += look*0.1 + (look*1.5 - vel)*0.5, component-wise.
        double expectedX = 1.0 + (0.0 * 0.1 + (0.0 * 1.5 - 1.0) * 0.5);
        double expectedY = 0.0 + (0.0 * 0.1 + (0.0 * 1.5 - 0.0) * 0.5);
        double expectedZ = 0.0 + (1.0 * 0.1 + (1.0 * 1.5 - 0.0) * 0.5);
        assertVecEquals(new Vec(expectedX, expectedY, expectedZ), next);
    }

    @DisplayName("nextVelocity() leaves a player already at the look-aligned target velocity unchanged in that direction")
    @Test
    void nextVelocityLeavesTheTargetVelocityStable() {
        // At vel == look * 1.5, both the acceleration and pull-towards-target terms are zero only
        // when look is also zero on that axis; pick a look-aligned velocity on a single axis to
        // keep the expectation simple.
        Vec look = new Vec(1.0, 0.0, 0.0);
        Vec velocity = new Vec(1.5, 0.0, 0.0);

        Vec next = FireworkBoostPhysics.nextVelocity(velocity, look);

        // vel.x += look.x*0.1 + (look.x*1.5 - vel.x)*0.5 = 0.1 + (1.5 - 1.5)*0.5 = 0.1
        assertVecEquals(new Vec(1.6, 0.0, 0.0), next);
    }

    @DisplayName("toPerTickVelocity() and toAppliedVelocity() (multiplier 1.0) are inverses of one another")
    @Test
    void perTickAndAppliedVelocityAreInversesAtVanillaMultiplier() {
        double tps = ServerFlag.SERVER_TICKS_PER_SECOND;
        Vec perSecond = new Vec(3.0, -1.5, 6.0);

        Vec perTick = FireworkBoostPhysics.toPerTickVelocity(perSecond, tps);
        Vec roundTripped = FireworkBoostPhysics.toAppliedVelocity(perTick, tps, 1.0);

        assertVecEquals(perSecond, roundTripped);
    }

    @DisplayName("toAppliedVelocity() scales linearly with the boost multiplier")
    @Test
    void toAppliedVelocityScalesLinearlyWithTheBoostMultiplier() {
        double tps = ServerFlag.SERVER_TICKS_PER_SECOND;
        Vec perTick = new Vec(0.2, 0.1, -0.3);

        Vec applied = FireworkBoostPhysics.toAppliedVelocity(perTick, tps, 35.0);

        assertVecEquals(perTick.mul(tps * 35.0), applied);
    }

    @DisplayName("lifetime() is deterministic for a seeded random and matches the vanilla formula")
    @Test
    void lifetimeIsDeterministicForASeededRandom() {
        long seed = 42L;
        Random forMethod = new Random(seed);
        Random forExpectation = new Random(seed);

        int lifetime = FireworkBoostPhysics.lifetime(3, forMethod);

        int expected = 10 * 3 + forExpectation.nextInt(6) + forExpectation.nextInt(7);
        Assertions.assertEquals(expected, lifetime);
    }

    @DisplayName("lifetime() scales with flightCount and stays within the vanilla random range")
    @Test
    void lifetimeStaysWithinTheVanillaRandomRangeForEveryFlightCount() {
        Random random = new Random(7L);

        for (int flightCount = 1; flightCount <= 5; flightCount++) {
            int lifetime = FireworkBoostPhysics.lifetime(flightCount, random);

            int lowerBound = 10 * flightCount;
            int upperBound = 10 * flightCount + 5 + 6;
            Assertions.assertTrue(lifetime >= lowerBound && lifetime <= upperBound, "lifetime " + lifetime + " for flightCount " + flightCount + " must be within [" + lowerBound + ", " + upperBound + "]");
        }
    }

    private static void assertVecEquals(Vec expected, Vec actual) {
        Assertions.assertEquals(expected.x(), actual.x(), TOLERANCE, "x component");
        Assertions.assertEquals(expected.y(), actual.y(), TOLERANCE, "y component");
        Assertions.assertEquals(expected.z(), actual.z(), TOLERANCE, "z component");
    }
}
