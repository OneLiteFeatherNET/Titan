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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;

class AscentPhaseTest {

    private static final CourseBlock START = TestBlocks.at(new BlockPos(0, 10, 0), Surface.FULL);
    private static final Heading EAST = new Heading(1.0, 0.0);

    private static RandomGenerator seeded(long seed) {
        return RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    }

    /** The five ascent jumps, or fewer when the generator runs out of room. */
    private static List<CourseBlock> ascent(FakeSpaceProbe world, Heading heading, long seed) {
        CourseGenerator generator = new CourseGenerator(world, seeded(seed));
        List<CourseBlock> course = new ArrayList<>(List.of(START));
        Phase phase = Phase.start(heading);
        while (phase instanceof Phase.Ascent) {
            Optional<CourseBlock> next = generator.next(course, phase);
            if (next.isEmpty()) {
                break;
            }
            course.add(next.get());
            phase = generator.after(course, phase);
        }
        return course;
    }

    // --- phase sequence ---------------------------------------------------------------------------

    @Test
    void startsWithNoJumpsMade() {
        assertEquals(new Phase.Ascent(0, EAST), Phase.start(EAST), "nothing made yet");
    }

    @Test
    void staysInTheAscentForAtLeastFiveJumpsEvenWithAirBelow() {
        Phase phase = Phase.start(EAST);
        for (int i = 0; i < Phase.MIN_ASCENT_JUMPS - 1; i++) {
            phase = ((Phase.Ascent) phase).next(true);
            assertInstanceOf(Phase.Ascent.class, phase, "jump " + (i + 1) + " is still ascent");
        }

        assertEquals(new Phase.Scored(0), ((Phase.Ascent) phase).next(true), "the fifth jump ends it once there is air below");
    }

    @Test
    void keepsAscendingAfterFiveJumpsWhileThereIsNoAirBelow() {
        assertEquals(new Phase.Ascent(6, EAST), new Phase.Ascent(5, EAST).next(false), "the sixth jump is still ascent");
    }

    @Test
    void endsTheAscentAsSoonAsTheBlockHasAirBelowAfterTheMinimum() {
        assertEquals(new Phase.Scored(0), new Phase.Ascent(7, EAST).next(true), "the eighth jump has air below");
    }

    @Test
    void generatesNoAscentJumpBeyondTheMaximum() {
        CourseGenerator generator = new CourseGenerator(new FakeSpaceProbe(), seeded(1L));

        assertTrue(generator.next(List.of(START), new Phase.Ascent(Phase.MAX_ASCENT_JUMPS, EAST)).isEmpty(), "twenty jumps are the limit");
    }

    @Test
    void scoredPhaseCountsUp() {
        assertEquals(new Phase.Scored(8), new Phase.Scored(7).next(), "score");
    }

    @Test
    void ascentJumpsAreShortAndUpwardOnFullBlocks() {
        Phase ascent = Phase.start(EAST);

        assertEquals(List.of(1, 2), ascent.gaps().boxed().toList(), "gaps");
        assertEquals(List.of(1), ascent.rises().boxed().toList(), "rises");
        assertEquals(List.of(Surface.FULL), ascent.surfaces(), "surfaces");
    }

    @Test
    void scoredJumpsMayHaveAnyGapRiseAndSurface() {
        Phase scored = new Phase.Scored(0);

        assertEquals(List.of(1, 2, 3, 4), scored.gaps().boxed().toList(), "gaps");
        assertEquals(List.of(-1, 0, 1), scored.rises().boxed().toList(), "rises");
        assertEquals(List.of(Surface.values()), scored.surfaces(), "surfaces");
    }

    // --- the ascent itself ------------------------------------------------------------------------

    @Test
    void ascentIsFiveFullBlocksEachOneHigherWithAShortGap() {
        List<CourseBlock> course = ascent(new FakeSpaceProbe(), EAST, 1L);

        assertEquals(Phase.MIN_ASCENT_JUMPS + 1, course.size(), "start plus five jumps");
        for (int i = 1; i < course.size(); i++) {
            Jump jump = new Jump(course.get(i - 1), course.get(i));
            assertEquals(Surface.FULL, jump.to().surface(), "full block at jump " + i);
            assertEquals(1.0, jump.rise(), "one block up at jump " + i);
            assertTrue(jump.gap() >= 1 && jump.gap() <= 2, "gap 1-2 at jump " + i + " but " + jump.gap());
        }
    }

    @Test
    void ascentLeadsAwayFromTheSpawn() {
        for (long seed = 0; seed < 30; seed++) {
            List<CourseBlock> course = ascent(new FakeSpaceProbe(), EAST, seed);
            for (int i = 1; i < course.size(); i++) {
                int dx = course.get(i).pos().x() - course.get(i - 1).pos().x();
                assertTrue(dx > 0, "jump " + i + " must go east, away from the spawn (seed " + seed + "), dx " + dx);
            }
        }
    }

    @Test
    void ascentVariesItsDirectionAmongThoseAwayFromTheSpawn() {
        long sidewaysJumps = 0;
        for (long seed = 0; seed < 30; seed++) {
            List<CourseBlock> course = ascent(new FakeSpaceProbe(), EAST, seed);
            for (int i = 1; i < course.size(); i++) {
                sidewaysJumps += course.get(i).pos().z() != course.get(i - 1).pos().z() ? 1 : 0;
            }
        }
        assertTrue(sidewaysJumps > 0, "diagonal steps should occur too");
    }

    @Test
    void ascentTurnsAwayFromAWallInTheWayOfTheSpawnDirection() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(3, 0, -50, 50, 100, 50);

        List<CourseBlock> course = ascent(world, EAST, 5L);

        assertEquals(Phase.MIN_ASCENT_JUMPS + 1, course.size(), "still room to ascend");
        for (CourseBlock block : course) {
            assertTrue(block.pos().x() < 3, "block inside the wall: " + block.pos());
        }
    }

    @Test
    void ascentFailsUnderALowCeiling() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(-50, 12, -50, 50, 12, 50);

        List<CourseBlock> course = ascent(world, EAST, 1L);

        assertEquals(1, course.size(), "not even the first jump fits under a ceiling two blocks above the start");
    }

    // --- ending in the open -------------------------------------------------------------------------

    private static boolean hasAirBelow(FakeSpaceProbe world, CourseBlock block) {
        return new Openness(world).hasAirBelow(block.pos());
    }

    @Test
    void ascentOnFlatGroundEndsAtTheFifthJumpWithAirBelow() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(-50, 0, -50, 50, 9, 50);

        List<CourseBlock> course = ascent(world, EAST, 1L);

        assertEquals(Phase.MIN_ASCENT_JUMPS + 1, course.size(), "start plus five jumps");
        assertTrue(hasAirBelow(world, course.getLast()), "four air blocks below the last ascent block");
    }

    @Test
    void ascentOnRisingGroundGoesOnUntilTheLastBlockHasAirBelow() {
        FakeSpaceProbe world = FakeSpaceProbe.risingGround(3);
        for (long seed = 0; seed < 10; seed++) {
            List<CourseBlock> course = ascent(world, EAST, seed);

            assertTrue(course.size() > Phase.MIN_ASCENT_JUMPS + 1, "the ground climbs along, so more than five jumps (seed " + seed + "), got " + (course.size() - 1));
            assertTrue(hasAirBelow(world, course.getLast()), "the last block must stand in the open (seed " + seed + ")");
            assertFalse(hasAirBelow(world, course.get(course.size() - 2)), "the one before it was not in the open yet (seed " + seed + ")");
        }
    }

    @Test
    void ascentLeavesTheStepsBeforeTheLastOneUncheckedForAirBelow() {
        FakeSpaceProbe world = FakeSpaceProbe.risingGround(3);

        List<CourseBlock> course = ascent(world, EAST, 2L);

        assertEquals(Surface.FULL, course.get(1).surface(), "even the first block, right above the ground, is built");
        assertFalse(hasAirBelow(world, course.get(1)), "and it has no air below");
    }

    @Test
    void ascentGivesUpWhenTheGroundClimbsAsFastAsTheBlocks() {
        FakeSpaceProbe world = FakeSpaceProbe.risingGround(2);

        List<CourseBlock> course = ascent(world, EAST, 1L);

        assertTrue(course.size() - 1 <= Phase.MAX_ASCENT_JUMPS, "never more than twenty jumps");
        assertFalse(hasAirBelow(world, course.getLast()), "the open was never reached");
    }

    // --- heading ----------------------------------------------------------------------------------

    @Test
    void headingPointsFromTheSpawnToThePlayer() {
        Heading heading = Heading.away(10.0, 0.0, 0.0, 0.0, 0.0, 1.0);

        assertEquals(1.0, heading.x(), 1e-9, "x");
        assertEquals(0.0, heading.z(), 1e-9, "z");
    }

    @Test
    void headingIsNormalized() {
        Heading heading = Heading.away(3.0, 4.0, 0.0, 0.0, 0.0, 1.0);

        assertEquals(1.0, Math.hypot(heading.x(), heading.z()), 1e-9, "unit length");
        assertEquals(0.6, heading.x(), 1e-9, "x");
    }

    @Test
    void headingFallsBackToTheFacingDirectionOnTheSpawn() {
        Heading heading = Heading.away(5.0, 5.0, 5.0, 5.0, 0.0, -2.0);

        assertEquals(0.0, heading.x(), 1e-9, "x");
        assertEquals(-1.0, heading.z(), 1e-9, "z");
    }

    @Test
    void headingFallsBackToTheFacingDirectionNextToTheSpawn() {
        Heading heading = Heading.away(5.02, 5.0, 5.0, 5.0, 0.0, 1.0);

        assertEquals(1.0, heading.z(), 1e-9, "facing, not the 2 cm offset");
    }

    @Test
    void headingHasADefaultWhenNeitherOffsetNorFacingGiveADirection() {
        Heading heading = Heading.away(5.0, 5.0, 5.0, 5.0, 0.0, 0.0);

        assertEquals(1.0, Math.hypot(heading.x(), heading.z()), 1e-9, "still a unit vector");
    }

    @Test
    void headingDotProductWeighsDirections() {
        assertEquals(1.0, EAST.dot(Direction.EAST), 1e-9, "same direction");
        assertEquals(-1.0, EAST.dot(Direction.WEST), 1e-9, "opposite");
        assertEquals(0.0, EAST.dot(Direction.SOUTH), 1e-9, "sideways");
        assertEquals(Math.sqrt(0.5), EAST.dot(Direction.SOUTH_EAST), 1e-9, "diagonal is normalized");
    }
}
