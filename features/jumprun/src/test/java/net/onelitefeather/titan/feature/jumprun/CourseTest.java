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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.Test;

class CourseTest {

    private static final BlockPos START_BLOCK = new BlockPos(0, 10, 0);
    private static final Pos START_POINT = new Pos(0.5, 11.0, 0.5, 90f, 0f);
    private static final Heading EAST = new Heading(1.0, 0.0);

    private static RandomGenerator seeded(long seed) {
        return RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    }

    private static Course startIn(FakeSpaceProbe world, long seed) {
        return TestBlocks.course(START_POINT, START_BLOCK, EAST, TestBlocks.FAR_SPAWN, world, seeded(seed)).orElseThrow();
    }

    private static Course start() {
        return startIn(new FakeSpaceProbe(), 1L);
    }

    /**
     * The course block {@code ahead} places after the current one; the window has it at 2 + ahead.
     */
    private static CourseBlock ahead(Course course, int ahead) {
        List<CourseBlock> window = course.window();
        return window.get(window.indexOf(course.current()) + ahead);
    }

    private static Course.Advance landOn(Course course, CourseBlock block) {
        return course.advanceTo(new Pos(block.pos().x() + 0.5, block.topY(), block.pos().z() + 0.5));
    }

    private static void landOnNext(Course course, int times) {
        for (int i = 0; i < times; i++) {
            landOn(course, ahead(course, 1));
        }
    }

    // --- start ------------------------------------------------------------------------------------

    @Test
    void startsWithTheStartBlockAndTwoAhead() {
        Course course = start();

        assertEquals(3, course.window().size(), "start block plus two ahead, nothing behind yet");
        assertEquals(START_BLOCK, course.window().getFirst().pos(), "the window begins at the start block");
        assertEquals(course.window().getFirst(), course.current(), "the start block is the current one");
    }

    @Test
    void startsWithScoreZero() {
        assertEquals(0, start().score(), "score");
    }

    @Test
    void keepsTheStartPointForTheResetAfterAFall() {
        assertEquals(START_POINT, start().startPoint(), "start point");
    }

    @Test
    void doesNotStartUnderALowCeiling() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(-50, 12, -50, 50, 12, 50);

        Optional<Course> course = TestBlocks.course(START_POINT, START_BLOCK, EAST, TestBlocks.FAR_SPAWN, world, seeded(1L));

        assertTrue(course.isEmpty(), "the ascent needs room");
    }

    @Test
    void sameSeedGivesTheSameWindowsAlongTheSameLandings() {
        Course first = startIn(new FakeSpaceProbe(), 9L);
        Course second = startIn(new FakeSpaceProbe(), 9L);

        for (int i = 0; i < 20; i++) {
            landOnNext(first, 1);
            landOnNext(second, 1);
            assertEquals(first.window(), second.window(), "window after landing " + (i + 1));
        }
    }

    @Test
    void doesNotStartWhenTheSpawnIsNotLeftWithinThirtyJumps() {
        FakeSpaceProbe walled = new FakeSpaceProbe(new BlockPos(-5, 0, -5), new BlockPos(5, 100, 5));
        SpawnZone spawn = new SpawnZone(0.5, 0.5);

        Optional<Course> course = TestBlocks.course(START_POINT, START_BLOCK, EAST, spawn, walled, seeded(1L));

        assertTrue(course.isEmpty(), "there is no room for a block sixteen blocks from the spawn");
    }

    @Test
    void startsFromTheOwnPositionAsSpawnWhenThereIsRoom() {
        SpawnZone ownPosition = new SpawnZone(START_POINT.x(), START_POINT.z());

        Optional<Course> course = TestBlocks.course(START_POINT, START_BLOCK, EAST, ownPosition, new FakeSpaceProbe(), seeded(1L));

        assertTrue(course.isPresent(), "a lobby without a known spawn still lets a run start in an open world");
    }

    @Test
    void doesNotStartWhenTheOpenIsNotReachedWithinThirtyJumps() {
        Optional<Course> course = TestBlocks.course(START_POINT, START_BLOCK, EAST, TestBlocks.FAR_SPAWN, FakeSpaceProbe.risingGroundAround(2), seeded(1L));

        assertTrue(course.isEmpty(), "ground that climbs as fast as the ascent in every direction never leaves room below");
    }

    @Test
    void aLongAscentStillScoresNothingUntilItsLastBlock() {
        Course course = startIn(FakeSpaceProbe.risingGround(3), 3L);
        int ascentJumps = 0;
        while (course.score() == 0 && ascentJumps < Phase.MAX_ASCENT_JUMPS + 1) {
            landOnNext(course, 1);
            ascentJumps++;
        }

        assertTrue(ascentJumps - 1 > Phase.MIN_ASCENT_JUMPS, "the rising ground needs more than five ascent jumps, got " + (ascentJumps - 1));
        assertEquals(1, course.score(), "the first jump after the last ascent block scores one");
    }

    // --- main heading -------------------------------------------------------------------------------

    @Test
    void theMainHeadingStartsAlongTheAscentHeading() {
        Heading heading = start().heading();

        assertTrue(heading.dot(Direction.EAST) > 0.5, "the ascent leads east, so does the heading, got " + heading);
        assertEquals(1.0, Math.hypot(heading.x(), heading.z()), 1e-9, "unit vector");
    }

    @Test
    void theMainHeadingBendsWithTheBlocksMadeAlongTheWay() {
        Course course = start();
        Heading atTheStart = course.heading();

        landOnNext(course, 200);

        assertNotEquals(atTheStart, course.heading(), "two hundred blocks later the heading has moved on");
        assertEquals(1.0, Math.hypot(course.heading().x(), course.heading().z()), 1e-9, "still a unit vector");
    }

    @Test
    void sameSeedGivesTheSameMainHeading() {
        Course first = startIn(new FakeSpaceProbe(), 9L);
        Course second = startIn(new FakeSpaceProbe(), 9L);

        landOnNext(first, 50);
        landOnNext(second, 50);

        assertEquals(first.heading(), second.heading(), "same seed, same heading");
    }

    // --- window -----------------------------------------------------------------------------------

    @Test
    void landingOnTheNextBlockShowsOneMoreAheadAndKeepsTheStart() {
        Course course = start();
        CourseBlock next = ahead(course, 1);
        CourseBlock newest = ahead(course, 2);

        Course.Advance advance = landOn(course, next);

        assertEquals(1, advance.jumps(), "one jump");
        assertEquals(4, course.window().size(), "start, current and two ahead");
        assertTrue(advance.removed().isEmpty(), "nothing left the window yet");
        assertEquals(1, advance.added().size(), "one new block");
        assertEquals(course.window().getLast(), advance.added().getFirst(), "the new block is the farthest");
        assertEquals(next, course.current(), "current");
        assertTrue(course.window().contains(newest), "the previously second ahead is still shown");
    }

    @Test
    void landingTwoBlocksAheadAdvancesBothJumps() {
        Course course = start();
        CourseBlock overNext = ahead(course, 2);

        Course.Advance advance = landOn(course, overNext);

        assertEquals(2, advance.jumps(), "both jumps count");
        assertEquals(overNext, course.current(), "current");
        assertEquals(2, advance.added().size(), "two new blocks ahead");
    }

    @Test
    void anAdvanceReportsOnlyTheScoredJumps() {
        Course course = start();

        Course.Advance advance = landOn(course, ahead(course, 2));

        assertEquals(course.score(), advance.scored(), "the score started at zero, so the advance carries all of it");
        assertTrue(advance.scored() <= advance.jumps(), "ascent jumps do not score");
    }

    @Test
    void theOldestBlockLeavesTheWindowAfterTheThirdLanding() {
        Course course = start();
        CourseBlock start = course.current();
        landOnNext(course, 2);
        CourseBlock beforeThird = course.window().getFirst();
        assertEquals(start, beforeThird, "the start block is still shown after two landings");

        Course.Advance advance = landOn(course, ahead(course, 1));

        assertEquals(List.of(start), advance.removed(), "the start block drops out");
        assertEquals(5, course.window().size(), "two behind, current, two ahead");
        assertFalse(course.window().contains(start), "start block gone");
    }

    @Test
    void windowNeverExceedsTwoBehindCurrentAndTwoAhead() {
        Course course = start();

        for (int i = 0; i < 300; i++) {
            landOnNext(course, 1);
            List<CourseBlock> window = course.window();
            assertTrue(window.size() <= 5, "window size " + window.size() + " after landing " + (i + 1));
            assertEquals(Math.min(2, i + 1), window.indexOf(course.current()), "at most two blocks behind, after landing " + (i + 1));
        }
    }

    @Test
    void addedAndRemovedBlocksMatchTheWindowChange() {
        Course course = start();
        List<CourseBlock> shown = new ArrayList<>(course.window());

        for (int i = 0; i < 50; i++) {
            Course.Advance advance = landOn(course, ahead(course, i % 3 == 0 ? 2 : 1));
            shown.removeAll(advance.removed());
            shown.addAll(advance.added());
            assertEquals(course.window(), shown, "replaying removed and added must give the window, step " + i);
        }
    }

    @Test
    void standingElsewhereDoesNotAdvance() {
        Course course = start();
        List<CourseBlock> before = course.window();

        Course.Advance advance = course.advanceTo(new Pos(40.5, 11.0, 40.5));

        assertEquals(0, advance.jumps(), "no jump");
        assertTrue(advance.added().isEmpty() && advance.removed().isEmpty(), "nothing changed");
        assertEquals(before, course.window(), "window");
    }

    @Test
    void standingOnTheCurrentOrAnEarlierBlockDoesNotAdvance() {
        Course course = start();
        landOnNext(course, 1);

        assertEquals(0, landOn(course, course.current()).jumps(), "current block");
        assertEquals(0, landOn(course, course.window().getFirst()).jumps(), "earlier block");
    }

    @Test
    void flyingAboveABlockDoesNotCountAsLanding() {
        Course course = start();
        CourseBlock next = ahead(course, 1);

        Course.Advance advance = course.advanceTo(new Pos(next.pos().x() + 0.5, next.topY() + 0.8, next.pos().z() + 0.5));

        assertEquals(0, advance.jumps(), "still in the air");
    }

    @Test
    void standingOnTheEdgeOfABlockCountsAsLanding() {
        Course course = start();
        CourseBlock next = ahead(course, 1);

        Course.Advance advance = course.advanceTo(new Pos(next.pos().x() - 0.25, next.topY(), next.pos().z() + 1.25));

        assertEquals(1, advance.jumps(), "the hitbox half width reaches over the edge");
    }

    @Test
    void standingBeyondTheHitboxHalfWidthOfABlockDoesNotCountAsLanding() {
        Course course = start();
        CourseBlock next = ahead(course, 1);

        Course.Advance advance = course.advanceTo(new Pos(next.pos().x() - 0.35, next.topY(), next.pos().z() + 0.5));

        assertEquals(0, advance.jumps(), "the hitbox does not touch the block");
    }

    @Test
    void landingOnASlabUsesItsOwnTop() {
        assertLandsOnItsOwnTop(Surface.SLAB);
    }

    @Test
    void landingOnAFenceUsesItsOwnTop() {
        assertLandsOnItsOwnTop(Surface.FENCE);
    }

    private static void assertLandsOnItsOwnTop(Surface surface) {
        Course course = start();
        for (int i = 0; i < 2000 && ahead(course, 1).surface() != surface; i++) {
            landOnNext(course, 1);
        }
        CourseBlock target = ahead(course, 1);
        assertEquals(surface, target.surface(), "a " + surface + " block must show up within 2000 jumps");

        Course.Advance advance = course.advanceTo(new Pos(target.pos().x() + 0.5, target.pos().y() + surface.top(), target.pos().z() + 0.5));

        assertEquals(1, advance.jumps(), surface + " top");
    }

    // --- score ------------------------------------------------------------------------------------

    @Test
    void theAscentDoesNotCountTowardsTheScore() {
        Course course = start();

        landOnNext(course, Phase.MIN_ASCENT_JUMPS);

        assertEquals(0, course.score(), "score after the five ascent jumps");
    }

    @Test
    void everyJumpAfterTheAscentScoresOne() {
        Course course = start();
        landOnNext(course, Phase.MIN_ASCENT_JUMPS + 1);

        assertEquals(1, course.score(), "first scored jump");

        landOnNext(course, 4);

        assertEquals(5, course.score(), "five scored jumps");
    }

    @Test
    void landingTwoAheadScoresBothJumps() {
        Course course = start();
        landOnNext(course, Phase.MIN_ASCENT_JUMPS);

        landOn(course, ahead(course, 2));

        assertEquals(2, course.score(), "both jumps score");
    }

    @Test
    void landingTwoAheadAcrossTheEndOfTheAscentScoresOnlyTheScoredJump() {
        Course course = start();
        landOnNext(course, Phase.MIN_ASCENT_JUMPS - 1);

        landOn(course, ahead(course, 2));

        assertEquals(1, course.score(), "one ascent jump, one scored jump");
    }

    // --- falling ----------------------------------------------------------------------------------

    @Test
    void fallThresholdIsThreeBlocksBelowTheTopOfTheCurrentBlock() {
        Course course = start();

        assertEquals(8.0, course.fallThreshold(), "start block top is 11.0");
    }

    @Test
    void fallThresholdFollowsTheCurrentBlock() {
        Course course = start();
        landOnNext(course, 1);

        assertEquals(course.current().topY() - 3.0, course.fallThreshold(), "threshold follows the current block");
    }

    @Test
    void fallThresholdReachesThreeBelowTheLowestVisibleBlockOnADescent() {
        Course course = start();
        int landings = 0;
        while (!hasLowerBlockAhead(course) && landings < 300) {
            landOnNext(course, 1);
            landings++;
        }

        assertTrue(hasLowerBlockAhead(course), "a descent shows up within " + landings + " landings");
        double lowest = course.window().stream().skip(course.window().indexOf(course.current())).mapToDouble(CourseBlock::topY).min().orElseThrow();
        assertEquals(lowest - 3.0, course.fallThreshold(), "a player on a lower block ahead stands above the threshold even when the landing there was not seen yet");
    }

    private static boolean hasLowerBlockAhead(Course course) {
        List<CourseBlock> window = course.window();
        return window.stream().skip(window.indexOf(course.current()) + 1L).anyMatch(block -> block.topY() < course.current().topY());
    }

    @Test
    void fallingExactlyThreeBlocksIsNotYetAFall() {
        Course course = start();

        assertFalse(course.hasFallen(8.0), "three below is still fine");
        assertTrue(course.hasFallen(7.99), "more than three below ends the run");
    }

    // --- running out of room ------------------------------------------------------------------------

    @Test
    void isNotExhaustedWhileABlockIsStillAheadEvenIfTheOneAfterItDoesNotFit() {
        FakeSpaceProbe world = new FakeSpaceProbe();
        Course course = startIn(world, 4L);
        world.seal();

        for (int landing = 1; landing < Phase.MIN_ASCENT_JUMPS + 1; landing++) {
            assertFalse(landOn(course, ahead(course, 1)).exhausted(), "a block is still ahead after landing " + landing);
        }
    }

    @Test
    void isExhaustedWhenLandingOnTheLastBlockAndNoneCanBeAdded() {
        FakeSpaceProbe world = new FakeSpaceProbe();
        Course course = startIn(world, 4L);
        world.seal();
        landOnNext(course, Phase.MIN_ASCENT_JUMPS);

        assertTrue(landOn(course, ahead(course, 1)).exhausted(), "no room for a block after the last one");
    }

    @Test
    void doesNotReportExhaustionWhileThereIsRoom() {
        Course course = start();

        for (int i = 0; i < 100; i++) {
            assertFalse(landOn(course, ahead(course, 1)).exhausted(), "open world, landing " + (i + 1));
        }
    }

    @Test
    void aSteeredCourseSnakesAroundTheSpawnInsteadOfLeadingAwayForever() {
        FakeSpaceProbe world = new FakeSpaceProbe(new BlockPos(-300, 0, -300), new BlockPos(300, 100, 300));
        SpawnZone spawn = new SpawnZone(0.5, 0.5);
        Pos startPoint = new Pos(40.5, 11.0, 0.5, 90f, 0f);
        Course course = Course.startSteered(startPoint, new BlockPos(40, 10, 0), EAST, spawn, world, seeded(3L), TestBlocks.shipped(), PortalClearance.NONE).orElseThrow();

        landOnNext(course, 120);

        double distance = Math.hypot(course.current().pos().x() + 0.5 - spawn.x(), course.current().pos().z() + 0.5 - spawn.z());
        assertTrue(distance < 80.0, "after 120 blocks the course is still around the spawn, not far away, got " + distance);
    }
}
