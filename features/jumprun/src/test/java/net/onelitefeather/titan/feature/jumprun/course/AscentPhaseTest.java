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
package net.onelitefeather.titan.feature.jumprun.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Collectors;
import net.minestom.server.coordinate.Pos;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;
import net.onelitefeather.titan.feature.jumprun.space.Direction;
import net.onelitefeather.titan.feature.jumprun.space.FakeSpaceProbe;
import net.onelitefeather.titan.feature.jumprun.space.Heading;
import net.onelitefeather.titan.feature.jumprun.space.OccupiedProbe;
import net.onelitefeather.titan.feature.jumprun.space.Openness;
import net.onelitefeather.titan.feature.jumprun.space.SpawnZone;
import org.junit.jupiter.api.Test;

class AscentPhaseTest {

    private static final CourseBlock START = TestBlocks.at(new BlockPos(0, 10, 0), Surface.FULL);
    private static final Heading EAST = new Heading(1.0, 0.0);

    private static RandomGenerator seeded(long seed) {
        return RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    }

    /** The five ascent jumps, or fewer when the generator runs out of room. */
    private static List<CourseBlock> ascent(FakeSpaceProbe world, Heading heading, long seed) {
        return ascent(world, TestBlocks.FAR_SPAWN, heading, seed);
    }

    private static List<CourseBlock> ascent(FakeSpaceProbe world, SpawnZone spawn, Heading heading, long seed) {
        CourseGenerator generator = TestBlocks.generator(world, spawn, seeded(seed));
        List<CourseBlock> course = new ArrayList<>(List.of(START));
        Phase phase = Phase.start(heading, Mode.MEDIUM);
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
        assertEquals(new Phase.Ascent(0, EAST), Phase.start(EAST, Mode.MEDIUM), "nothing made yet");
    }

    @Test
    void staysInTheAscentForAtLeastFiveJumpsEvenWithAirBelow() {
        Phase phase = Phase.start(EAST, Mode.MEDIUM);
        for (int i = 0; i < Phase.MIN_ASCENT_JUMPS - 1; i++) {
            phase = ((Phase.Ascent) phase).next(true);
            assertInstanceOf(Phase.Ascent.class, phase, "jump " + (i + 1) + " is still ascent");
        }

        assertEquals(new Phase.Scored(0, EAST), ((Phase.Ascent) phase).next(true), "the fifth jump ends it once there is air below");
    }

    @Test
    void keepsAscendingAfterFiveJumpsWhileThereIsNoAirBelow() {
        assertEquals(new Phase.Ascent(6, EAST), new Phase.Ascent(5, EAST).next(false), "the sixth jump is still ascent");
    }

    @Test
    void endsTheAscentAsSoonAsTheBlockHasAirBelowAfterTheMinimum() {
        assertEquals(new Phase.Scored(0, EAST), new Phase.Ascent(7, EAST).next(true), "the eighth jump has air below");
    }

    @Test
    void generatesNoAscentJumpBeyondTheMaximum() {
        CourseGenerator generator = TestBlocks.generator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, seeded(1L));

        assertTrue(generator.next(List.of(START), new Phase.Ascent(Phase.MAX_ASCENT_JUMPS, EAST)).isEmpty(), "thirty jumps are the limit");
    }

    @Test
    void scoredPhaseCountsUp() {
        assertEquals(new Phase.Scored(8, EAST), new Phase.Scored(7, EAST).next(), "score");
    }

    @Test
    void ascentJumpsAreShortAndUpwardOnFullBlocks() {
        Phase ascent = Phase.start(EAST, Mode.MEDIUM);

        assertEquals(List.of(1, 2), ascent.gaps().boxed().toList(), "gaps");
        assertEquals(List.of(1), ascent.rises().boxed().toList(), "rises");
        assertEquals(List.of(Surface.FULL), ascent.surfaces(), "surfaces");
    }

    @Test
    void scoredJumpsMayHaveAnyGapRiseAndSurfaceOnceAllShapesAreUnlocked() {
        // The narrow shapes unlock one by one (D15); score 40 is the first with all of them.
        Phase scored = new Phase.Scored(40, EAST);

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
        // The main heading bends with the ascent (D15), so a jump may run sideways but never back.
        for (long seed = 0; seed < 30; seed++) {
            List<CourseBlock> course = ascent(new FakeSpaceProbe(), EAST, seed);
            for (int i = 1; i < course.size(); i++) {
                int dx = course.get(i).pos().x() - course.get(i - 1).pos().x();
                assertTrue(dx >= 0, "jump " + i + " must not go back towards the spawn (seed " + seed + "), dx " + dx);
            }
            assertTrue(course.getLast().pos().x() > course.getFirst().pos().x(), "the ascent as a whole goes east (seed " + seed + ")");
        }
    }

    @Test
    void ascentBendsItsHeadingTowardsEveryStep() {
        CourseGenerator generator = TestBlocks.generator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, seeded(3L));
        List<CourseBlock> course = new ArrayList<>(List.of(START));
        Phase phase = Phase.start(EAST, Mode.MEDIUM);

        course.add(generator.next(course, phase).orElseThrow());
        Phase after = generator.after(course, phase);

        Direction step = new Jump(course.get(0), course.get(1)).direction();
        assertEquals(EAST.steered(step), after.heading(), "the phase after a block carries the heading bent towards its step");
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

    /** The way the generator sees it: the four blocks made just before also take up room. */
    private static boolean hasAirBelow(FakeSpaceProbe world, List<CourseBlock> course, int index) {
        Set<BlockPos> earlier = course.subList(Math.max(0, index - Course.VISIBLE_BEFORE_NEW), index).stream().map(CourseBlock::pos).collect(Collectors.toSet());
        return new Openness(new OccupiedProbe(world, earlier)).hasAirBelow(course.get(index).pos(), Openness.ASCENT_AIR_BELOW);
    }

    private static boolean isInTheOpen(FakeSpaceProbe world, SpawnZone spawn, List<CourseBlock> course, int index) {
        return spawn.isFarEnough(course.get(index).pos()) && hasAirBelow(world, course, index);
    }

    @Test
    void ascentOnFlatGroundEndsAtTheFirstBlockWithEightAirBlocksBelow() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(-50, 0, -50, 50, 9, 50);

        List<CourseBlock> course = ascent(world, EAST, 1L);

        assertEquals(Openness.ASCENT_AIR_BELOW, course.size() - 1, "the start stands on the ground and each jump is one higher, so the eighth block has eight air blocks below");
        assertTrue(hasAirBelow(world, course, course.size() - 1), "eight air blocks below the last ascent block");
    }

    @Test
    void ascentOnRisingGroundGoesOnUntilTheLastBlockHasAirBelow() {
        FakeSpaceProbe world = FakeSpaceProbe.risingGround(3);
        for (long seed = 0; seed < 10; seed++) {
            List<CourseBlock> course = ascent(world, EAST, seed);

            assertTrue(course.size() > Phase.MIN_ASCENT_JUMPS + 1, "the ground climbs along, so more than five jumps (seed " + seed + "), got " + (course.size() - 1));
            assertTrue(hasAirBelow(world, course, course.size() - 1), "the last block must stand in the open (seed " + seed + ")");
            assertFalse(hasAirBelow(world, course, course.size() - 2), "the one before it was not in the open yet (seed " + seed + ")");
        }
    }

    @Test
    void ascentLeavesTheStepsBeforeTheLastOneUncheckedForAirBelow() {
        FakeSpaceProbe world = FakeSpaceProbe.risingGround(3);

        List<CourseBlock> course = ascent(world, EAST, 2L);

        assertEquals(Surface.FULL, course.get(1).surface(), "even the first block, right above the ground, is built");
        assertFalse(hasAirBelow(world, course, 1), "and it has no air below");
    }

    @Test
    void ascentGivesUpWhenTheGroundClimbsAsFastAsTheBlocks() {
        // Ground that rises in every direction: a sideways step no longer escapes it.
        FakeSpaceProbe world = FakeSpaceProbe.risingGroundAround(2);

        List<CourseBlock> course = ascent(world, EAST, 1L);

        assertTrue(course.size() - 1 <= Phase.MAX_ASCENT_JUMPS, "never more than thirty jumps");
        assertFalse(hasAirBelow(world, course, course.size() - 1), "the open was never reached");
    }

    // --- distance to the spawn -----------------------------------------------------------------------

    @Test
    void ascentGoesOnUntilTheLastBlockIsSixteenBlocksFromTheSpawn() {
        FakeSpaceProbe world = new FakeSpaceProbe();
        SpawnZone spawn = new SpawnZone(-3.5, 0.5);
        for (long seed = 0; seed < 10; seed++) {
            List<CourseBlock> course = ascent(world, spawn, EAST, seed);

            assertTrue(isInTheOpen(world, spawn, course, course.size() - 1), "the last block must be far from the spawn, in the open (seed " + seed + ")");
            for (int index = Phase.MIN_ASCENT_JUMPS; index < course.size() - 1; index++) {
                assertFalse(isInTheOpen(world, spawn, course, index), "block " + index + " was already far and open, the ascent should have ended (seed " + seed + ")");
            }
        }
    }

    @Test
    void ascentNeedsMoreJumpsCloseToTheSpawn() {
        FakeSpaceProbe world = new FakeSpaceProbe();
        List<CourseBlock> near = ascent(world, new SpawnZone(0.5, 0.5), EAST, 1L);
        List<CourseBlock> far = ascent(world, TestBlocks.FAR_SPAWN, EAST, 1L);

        assertTrue(near.size() > far.size(), "close to the spawn the ascent needs more jumps: " + near.size() + " vs " + far.size());
    }

    @Test
    void ascentFromTheOwnPositionAsSpawnLeavesItWithinThirtyJumps() {
        SpawnZone ownPosition = new SpawnZone(START.pos().x() + 0.5, START.pos().z() + 0.5);

        List<CourseBlock> course = ascent(new FakeSpaceProbe(), ownPosition, EAST, 1L);

        assertTrue(course.size() - 1 <= Phase.MAX_ASCENT_JUMPS, "at most thirty jumps, got " + (course.size() - 1));
        assertTrue(ownPosition.isFarEnough(course.getLast().pos()), "the last ascent block is sixteen blocks from where the run began");
    }

    @Test
    void ascentNeverReachesTheSpawnDistanceInAWorldTooSmall() {
        FakeSpaceProbe walled = new FakeSpaceProbe(new BlockPos(-5, 0, -5), new BlockPos(5, 100, 5));

        List<CourseBlock> course = ascent(walled, new SpawnZone(0.5, 0.5), EAST, 1L);

        assertTrue(course.stream().skip(1).noneMatch(block -> new SpawnZone(0.5, 0.5).isFarEnough(block.pos())), "a world of eleven blocks never gets sixteen from the spawn");
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
    void headingOnTheSpawnItselfIsFiniteForAnyFacing() {
        Heading facing = Heading.away(7.5, 7.5, 7.5, 7.5, 3.0, 4.0);
        Heading blind = Heading.away(7.5, 7.5, 7.5, 7.5, 0.0, 0.0);

        assertEquals(0.6, facing.x(), 1e-9, "identical points fall back to the facing direction without dividing by zero");
        assertTrue(Double.isFinite(blind.x()) && Double.isFinite(blind.z()), "no NaN without a facing direction either");
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

    // --- the height band ----------------------------------------------------------------------------

    private static Optional<Course> startIn(int minHeight, int maxHeight, long seed) {
        Pos startPoint = new Pos(0.5, 11.0, 0.5);
        return TestBlocks.course(startPoint, new BlockPos(0, 10, 0), EAST, TestBlocks.FAR_SPAWN, new FakeSpaceProbe(), new HeightBand(TestBlocks.bounds(minHeight, maxHeight)), seeded(seed));
    }

    @Test
    void startsWhenTheWholeAscentFitsUnderTheUpperLimit() {
        assertTrue(startIn(-64, 19, 1L).isPresent(), "the fifth ascent block has top 16 and its jump peaks at 18.26, under 19");
    }

    @Test
    void startsNoRunWhenTheAscentWouldPassTheUpperLimit() {
        assertTrue(startIn(-64, 18, 1L).isEmpty(), "the fifth ascent block's jump peaks at 18.26, over 18");
    }

    @Test
    void startsNoRunSixBlocksUnderTheUpperLimit() {
        assertTrue(startIn(-64, 16, 1L).isEmpty(), "the ascent climbs five blocks, so the band is left");
    }

    @Test
    void startsNoRunJustAboveTheLowerLimit() {
        assertTrue(startIn(3, 310, 1L).isEmpty(), "top 11 - 3 - 5 = 3 does not stay above 3");
    }

    @Test
    void startsWhenTheStartBlockKeepsTheFallAllowanceAboveTheLowerLimit() {
        assertTrue(startIn(2, 310, 1L).isPresent(), "top 11 - 3 - 5 = 3 stays above 2");
    }

    @Test
    void startsAsBeforeInsideTheShippedBand() {
        assertTrue(startIn(-64, 310, 1L).isPresent(), "a normal start is unchanged");
    }
}
