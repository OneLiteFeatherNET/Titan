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
package net.onelitefeather.titan.feature.jumprun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import net.onelitefeather.titan.feature.jumprun.Steering.Around;
import org.junit.jupiter.api.Test;

class SteeringTest {

    private static final SpawnZone SPAWN = new SpawnZone(0.5, 0.5);
    private static final double EPS = 1e-9;

    /** Phase 0 and count 0 mean the pendulum stands still in the middle. */
    private static Around steady(boolean clockwise) {
        return new Around(SPAWN, clockwise, 0.0);
    }

    /** A block cell {@code distance} blocks east of the spawn. */
    private static BlockPos east(int distance) {
        return new BlockPos(distance, 10, 0);
    }

    private static RandomGenerator seeded(long seed) {
        return RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    }

    @Test
    void inTheRingTheWishIsTheTangentOfTheCircleAroundTheSpawn() {
        Heading wish = steady(true).wish(east(40), 0);

        assertEquals(0.0, wish.x(), EPS, "no radial part");
        assertEquals(1.0, Math.abs(wish.z()), EPS, "along the circle");
    }

    @Test
    void theTwoSensesLeadOppositeWaysAroundTheSpawn() {
        Heading clockwise = steady(true).wish(east(40), 0);
        Heading counter = steady(false).wish(east(40), 0);

        assertEquals(-clockwise.z(), counter.z(), EPS, "opposite tangents");
        assertEquals(1.0, Math.abs(clockwise.z()), EPS, "a unit vector");
    }

    @Test
    void theTangentTurnsWithThePositionAroundTheSpawn() {
        Heading wish = steady(true).wish(new BlockPos(0, 10, 40), 0);

        assertEquals(1.0, Math.abs(wish.x()), 0.01, "north or south of the spawn the circle runs east-west");
        assertEquals(0.0, wish.z(), 0.02, "no radial part");
    }

    @Test
    void belowTheRingTheWishPullsOutwardAndAboveItInward() {
        double tooNear = steady(true).wish(east(18), 0).x();
        double inside = steady(true).wish(east(40), 0).x();
        double tooFar = steady(true).wish(east(70), 0).x();

        assertTrue(tooNear > 0.5, "18 blocks out pulls away from the spawn, got " + tooNear);
        assertEquals(0.0, inside, EPS, "40 blocks out has no radial pull");
        assertTrue(tooFar < -0.5, "70 blocks out pulls back towards the spawn, got " + tooFar);
    }

    @Test
    void theRadialPullGrowsLinearlyTowardsTheEdgesAndStopsAtFullStrength() {
        assertEquals(0.0, Around.radialPull(24.0), EPS, "none at 24");
        assertEquals(0.5, Around.radialPull(22.0), EPS, "half at 22");
        assertEquals(1.0, Around.radialPull(20.0), EPS, "full at 20");
        assertEquals(1.0, Around.radialPull(5.0), EPS, "capped below the ring");
        assertEquals(0.0, Around.radialPull(56.0), EPS, "none at 56");
        assertEquals(-0.5, Around.radialPull(58.0), EPS, "half at 58");
        assertEquals(-1.0, Around.radialPull(60.0), EPS, "full at 60");
        assertEquals(-1.0, Around.radialPull(90.0), EPS, "capped above the ring");
    }

    @Test
    void thePendulumSwingsFiftyDegreesToEitherSide() {
        Around steering = steady(true);
        double most = 0.0;
        double least = 0.0;
        for (int count = 0; count < Around.PENDULUM_PERIOD; count++) {
            most = Math.max(most, steering.swing(count));
            least = Math.min(least, steering.swing(count));
        }

        assertEquals(Math.toRadians(50.0), most, Math.toRadians(3.0), "to one side");
        assertEquals(-Math.toRadians(50.0), least, Math.toRadians(3.0), "to the other");
    }

    @Test
    void thePendulumRepeatsAfterFourteenBlocks() {
        Around steering = new Around(SPAWN, true, 1.3);

        for (int count = 0; count < 30; count++) {
            assertEquals(steering.swing(count), steering.swing(count + Around.PENDULUM_PERIOD), EPS, "count " + count);
        }
    }

    @Test
    void thePhaseShiftsThePendulum() {
        assertEquals(0.0, new Around(SPAWN, true, 0.0).swing(0), EPS, "starts in the middle");
        assertEquals(Around.PENDULUM_AMPLITUDE, new Around(SPAWN, true, Math.PI / 2.0).swing(0), EPS, "starts at the end");
    }

    @Test
    void theSwingTurnsTheWishAwayFromTheTangentByItsAngle() {
        Around steering = new Around(SPAWN, true, Math.PI / 2.0);
        Heading tangent = steady(true).wish(east(40), 0);

        Heading swung = steering.wish(east(40), 0);

        assertEquals(Math.toRadians(50.0), Math.acos(tangent.x() * swung.x() + tangent.z() * swung.z()), 1e-6, "angle between tangent and wish");
    }

    @Test
    void theWishIsAUnitVectorEverywhere() {
        Around steering = new Around(SPAWN, false, 0.7);
        for (int distance = 0; distance <= 100; distance += 7) {
            Heading wish = steering.wish(east(distance), distance);

            assertEquals(1.0, Math.hypot(wish.x(), wish.z()), EPS, "at " + distance);
        }
    }

    @Test
    void followingPullsTheHeadingAQuarterTowardsTheWish() {
        Around steering = steady(true);
        Heading wish = steering.wish(east(40), 0);
        Heading heading = new Heading(1.0, 0.0);

        Heading followed = steering.follow(heading, Direction.EAST, east(40), 0);

        double length = Math.hypot(0.75 + 0.25 * wish.x(), 0.25 * wish.z());
        assertEquals((0.75 + 0.25 * wish.x()) / length, followed.x(), EPS, "x");
        assertEquals(0.25 * wish.z() / length, followed.z(), EPS, "z");
    }

    @Test
    void theSameSeedDrawsTheSameSteering() {
        assertEquals(Steering.around(SPAWN, seeded(7L)), Steering.around(SPAWN, seeded(7L)), "same seed, same sense and phase");
    }

    @Test
    void seedsDrawBothSensesWithAPhaseWithinOneTurn() {
        boolean clockwise = false;
        boolean counter = false;
        for (long seed = 0; seed < 40; seed++) {
            Around around = (Around) Steering.around(SPAWN, seeded(seed));
            clockwise |= around.clockwise();
            counter |= !around.clockwise();
            assertTrue(around.phase() >= 0.0 && around.phase() < 2.0 * Math.PI, "phase in one turn, seed " + seed);
        }

        assertTrue(clockwise && counter, "both senses occur");
    }

    @Test
    void drawsTheSenseFirstAndThePhaseSecond() {
        RandomGenerator replay = seeded(5L);
        boolean sense = replay.nextBoolean();
        double phase = replay.nextDouble() * 2.0 * Math.PI;

        assertEquals(new Around(SPAWN, sense, phase), Steering.around(SPAWN, seeded(5L)), "the documented order");
    }

    @Test
    void noSteeringBendsTheHeadingTowardsTheStepLikeBefore() {
        Heading heading = new Heading(1.0, 0.0);

        assertEquals(heading.steered(Direction.SOUTH), Steering.none().follow(heading, Direction.SOUTH, east(40), 3), "plain smoothing");
    }
}
