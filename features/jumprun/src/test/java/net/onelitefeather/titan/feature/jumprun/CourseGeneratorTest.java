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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CourseGeneratorTest {

    private static final CourseBlock SOURCE = TestBlocks.at(new BlockPos(0, 10, 0), Surface.FULL);
    private static final Heading EAST = new Heading(1.0, 0.0);
    private static final Heading WEST = new Heading(-1.0, 0.0);
    private static final Heading NORTH = new Heading(0.0, -1.0);

    private static RandomGenerator seeded(long seed) {
        return RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    }

    /** Seeded randomness whose Gaussian noise is pinned, so the target cost is exactly known. */
    private static RandomGenerator withNoise(double noise, long seed) {
        RandomGenerator base = seeded(seed);
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return base.nextLong();
            }

            @Override
            public double nextGaussian() {
                return noise;
            }
        };
    }

    private static CourseBlock jumpFromSource(CourseGenerator generator, int score) {
        return jumpFromSource(generator, score, EAST);
    }

    private static CourseBlock jumpFromSource(CourseGenerator generator, int score, Heading heading) {
        return generator.next(List.of(SOURCE), new Phase.Scored(score, heading)).orElseThrow();
    }

    /** A jump as the generator made it, with the phase it was made in. */
    private record Move(Jump jump, Phase phase) {

        Heading heading() {
            return phase.heading();
        }
    }

    /** Walks the generator forward from the start and returns every jump made on the way. */
    private static List<Move> moves(CourseGenerator generator, CourseBlock start, Phase phase, int jumps) {
        List<CourseBlock> course = new ArrayList<>(List.of(start));
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < jumps; i++) {
            Optional<CourseBlock> next = generator.next(course, phase);
            if (next.isEmpty()) {
                break;
            }
            moves.add(new Move(new Jump(course.getLast(), next.get()), phase));
            course.add(next.get());
            phase = generator.after(course, phase);
        }
        return moves;
    }

    /** Walks the generator forward from the start and returns the whole course, start included. */
    private static List<CourseBlock> walk(CourseGenerator generator, CourseBlock start, Phase phase, int jumps) {
        List<CourseBlock> course = new ArrayList<>(List.of(start));
        moves(generator, start, phase, jumps).forEach(move -> course.add((CourseBlock) move.jump().to()));
        return course;
    }

    // --- choosing by cost -----------------------------------------------------------------------

    @Test
    void picksTheCandidateWithTheTargetCostWhenTheTargetIsZero() {
        CourseGenerator generator = new CourseGenerator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, withNoise(-100.0, 1L));

        CourseBlock chosen = jumpFromSource(generator, 0);

        assertEquals(0.0, new Jump(SOURCE, chosen).cost(), "an easiest jump");
    }

    @Test
    void picksTheCandidateClosestToTheTargetCost() {
        // Slabs and trapdoors unlock at score 10 (D15); at 0 only full blocks cost 1.5 or 2.5, never 2.0.
        double noise = 2.0 - Difficulty.level(10) * Jump.maxCost(Surface.unlockedAt(10));
        CourseGenerator generator = new CourseGenerator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, withNoise(noise, 1L));

        CourseBlock chosen = jumpFromSource(generator, 10);

        assertEquals(2.0, new Jump(SOURCE, chosen).cost(), "target 2.0 is met exactly by a slab or a trapdoor");
        assertTrue(Set.of(Surface.SLAB, Surface.TRAPDOOR).contains(chosen.surface()), "only these cost 2.0 without a gap or a rise, got " + chosen.surface());
    }

    @Test
    void whenCostsTieTheCandidateInTheMainHeadingWins() {
        for (long seed = 0; seed < 30; seed++) {
            CourseGenerator generator = new CourseGenerator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, withNoise(-100.0, seed));

            BlockPos chosen = jumpFromSource(generator, 0).pos();

            assertTrue(chosen.x() > 0 && chosen.z() == 0, "east is the heading, the sideways jumps cost the same (seed " + seed + "), chose " + chosen);
        }
    }

    @Test
    void aStepAgainstTheMainHeadingIsNeverChosen() {
        for (long seed = 0; seed < 30; seed++) {
            CourseGenerator generator = new CourseGenerator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, withNoise(100.0, seed));

            BlockPos chosen = jumpFromSource(generator, 10_000).pos();

            assertTrue(chosen.x() >= 0, "the hardest jump may not lead west against an eastern heading (seed " + seed + "), chose " + chosen);
        }
    }

    @Test
    void picksTheHardestJumpWhenTheTargetIsAboveEverything() {
        CourseGenerator generator = new CourseGenerator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, withNoise(100.0, 1L));

        CourseBlock chosen = jumpFromSource(generator, 10_000);

        assertEquals(Jump.MAX_COST, new Jump(SOURCE, chosen).cost(), "hardest allowed jump");
    }

    // --- space ----------------------------------------------------------------------------------

    @Test
    void rejectsACandidateWithoutAnyFollowUpJump() {
        for (long seed = 0; seed < 20; seed++) {
            // A is the easiest candidate but nothing is reachable from it; B starts a way west
            // that goes on for more blocks than the dead-end check looks ahead.
            FakeSpaceProbe world = FakeSpaceProbe.solidWorld().carveColumn(2, 3, 0, 12).carveColumn(1, 11, 0, 2);
            for (int x = -35; x <= -5; x += 5) {
                world.carveColumn(x, 3, 0, 12);
            }
            for (int x = -34; x <= -1; x++) {
                world.carveColumn(x, 11, 0, 2);
            }
            CourseGenerator generator = new CourseGenerator(world, TestBlocks.FAR_SPAWN, withNoise(0.0, seed));

            CourseBlock chosen = jumpFromSource(generator, 0, NORTH);

            assertTrue(chosen.pos().x() < 0, "dead end A in the east must lose to the way west (seed " + seed + "), chose " + chosen.pos());
        }
    }

    @Test
    void endsTheCourseWhenTheOnlyFittingBlockLeadsIntoADeadEnd() {
        for (long seed = 0; seed < 20; seed++) {
            // The block in the east is free, but nothing can be reached from it.
            FakeSpaceProbe world = FakeSpaceProbe.solidWorld().carveColumn(2, 3, 0, 12).carveColumn(1, 11, 0, 2);
            CourseGenerator generator = new CourseGenerator(world, TestBlocks.FAR_SPAWN, withNoise(0.0, seed));

            Optional<CourseBlock> next = generator.next(List.of(SOURCE), new Phase.Scored(0, EAST));

            assertTrue(next.isEmpty(), "a dead end must not be offered, so the run is exhausted (seed " + seed + "), got " + next);
        }
    }

    @Test
    void refusesAPhaseAfterACourseOfOneBlock() {
        CourseGenerator generator = new CourseGenerator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, seeded(1L));

        assertThrows(IllegalArgumentException.class, () -> generator.after(List.of(SOURCE), new Phase.Scored(0, EAST)), "there is no jump to steer by yet");
    }

    @Test
    void neverPlacesAScoredBlockOverAWay() {
        // From y=10 a block reaches at most y=11, which leaves no six air blocks above the way at y<=7.
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(0, 0, -50, 50, 7, 50);
        for (long seed = 0; seed < 30; seed++) {
            CourseBlock chosen = jumpFromSource(new CourseGenerator(world, TestBlocks.FAR_SPAWN, seeded(seed)), 0, WEST);

            assertTrue(chosen.pos().x() < 0, "only the open side may be used (seed " + seed + "), chose " + chosen.pos());
        }
    }

    @Test
    void placesNoScoredBlockWithFewerThanSixAirBlocksBelow() {
        // The way tops out at y=4: a block at y=10 has five air blocks below, one at y=11 has six.
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(-50, 0, -50, 50, 4, 50);
        for (long seed = 0; seed < 30; seed++) {
            CourseBlock chosen = jumpFromSource(new CourseGenerator(world, TestBlocks.FAR_SPAWN, seeded(seed)), 0);

            assertEquals(11, chosen.pos().y(), "the only height with six air blocks below (seed " + seed + ")");
        }
    }

    @Test
    void placesNoScoredBlockWithinSixteenBlocksOfTheSpawn() {
        SpawnZone onTheStart = new SpawnZone(0.5, 0.5);

        assertTrue(new CourseGenerator(new FakeSpaceProbe(), onTheStart, seeded(1L)).next(List.of(SOURCE), new Phase.Scored(0, EAST)).isEmpty(), "every jump of at most five blocks stays near the spawn");
    }

    @Test
    void placesScoredBlocksOnlyWhereTheSpawnIsSixteenBlocksAway() {
        SpawnZone behind = new SpawnZone(-12.5, 0.5);
        for (long seed = 0; seed < 30; seed++) {
            CourseBlock chosen = jumpFromSource(new CourseGenerator(new FakeSpaceProbe(), behind, seeded(seed)), 0);

            assertTrue(behind.isFarEnough(chosen.pos()), "chose " + chosen.pos() + " near the spawn (seed " + seed + ")");
        }
    }

    @Test
    void prefersMoreAirBelowOfTwoEquallyCostlyPlaces() {
        // Ground below the east side, a wall in the north: east and west cost and lead the same,
        // but every place in the east has fewer air blocks in its column.
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(1, 0, -50, 50, 3, 50).occupyBox(-50, 0, -2, 50, 100, -2);
        for (long seed = 0; seed < 30; seed++) {
            CourseBlock chosen = jumpFromSource(new CourseGenerator(world, TestBlocks.FAR_SPAWN, withNoise(-100.0, seed)), 0, NORTH);

            assertTrue(chosen.pos().x() < 0, "the place with the deeper column wins at equal cost (seed " + seed + "), chose " + chosen.pos());
        }
    }

    @Test
    void reportsNoCandidateWhenEveryPlaceIsOverAWay() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(-50, 0, -50, 50, 7, 50);

        assertTrue(new CourseGenerator(world, TestBlocks.FAR_SPAWN, seeded(1L)).next(List.of(SOURCE), new Phase.Scored(0, EAST)).isEmpty(), "ground three blocks below the start leaves no room");
    }

    @Test
    void prefersTheMoreOpenOfTwoEquallyCostlyPlaces() {
        // East and west offer the same four zero-cost jumps; a wall hugs the west ones.
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(0, 8, 2, 0, 12, 2).occupyBox(0, 8, -2, 0, 12, -2).occupyBox(-3, 0, -50, -3, 100, 50);
        for (long seed = 0; seed < 30; seed++) {
            CourseBlock chosen = jumpFromSource(new CourseGenerator(world, TestBlocks.FAR_SPAWN, withNoise(-100.0, seed)), 0, NORTH);

            assertTrue(chosen.pos().x() > 0, "the open side wins at equal cost (seed " + seed + "), chose " + chosen.pos());
        }
    }

    @Test
    void neverPlacesABlockIntoTheHeadroomOfAnEarlierBlock() {
        CourseBlock earlier = TestBlocks.at(new BlockPos(0, 10, 0), Surface.FULL);
        CourseBlock last = TestBlocks.at(new BlockPos(2, 10, 0), Surface.FULL);
        Phase towardsTheEarlierBlock = new Phase.Ascent(Phase.MIN_ASCENT_JUMPS, new Heading(-1.0, 0.0));

        for (long seed = 0; seed < 200; seed++) {
            CourseGenerator generator = new CourseGenerator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, seeded(seed));

            BlockPos chosen = generator.next(List.of(earlier, last), towardsTheEarlierBlock).orElseThrow().pos();

            boolean inItsColumn = chosen.x() == 0 && chosen.z() == 0 && chosen.y() <= earlier.headroomTopY();
            assertFalse(inItsColumn, "inside the earlier block or its headroom: " + chosen + " (seed " + seed + ")");
        }
    }

    @Test
    void reportsNoCandidateWhenNothingIsFree() {
        CourseGenerator generator = new CourseGenerator(FakeSpaceProbe.solidWorld(), TestBlocks.FAR_SPAWN, seeded(1L));

        assertTrue(generator.next(List.of(SOURCE), new Phase.Scored(0, EAST)).isEmpty(), "walled in");
    }

    @Test
    void leadsAroundAWallInsteadOfThroughIt() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(6, 0, -50, 50, 100, 50);
        CourseGenerator generator = new CourseGenerator(world, TestBlocks.FAR_SPAWN, seeded(3L));

        List<CourseBlock> course = walk(generator, SOURCE, new Phase.Scored(0, EAST), 60);

        assertTrue(course.size() > 40, "the course must keep going, got " + course.size());
        for (CourseBlock block : course) {
            assertTrue(block.pos().x() < 6, "block inside the wall: " + block.pos());
        }
    }

    @Test
    void doesNotRepeatAPositionOfTheLastFourBlocks() {
        CourseGenerator generator = new CourseGenerator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, seeded(11L));

        List<CourseBlock> course = walk(generator, SOURCE, new Phase.Scored(0, EAST), 300);

        for (int i = 1; i < course.size(); i++) {
            for (int back = 1; back <= 4 && back <= i; back++) {
                assertNotEquals(course.get(i - back).pos(), course.get(i).pos(), "block " + i + " repeats the one " + back + " before it");
            }
        }
    }

    // --- snaking around the spawn (D17) -----------------------------------------------------------

    private static final SpawnZone MIDDLE_SPAWN = new SpawnZone(0.5, 0.5);
    private static final int SNAKE_POINTS = 60;
    private static final long SNAKE_SEED = 11L;

    private static List<CourseBlock> snake(long seed) {
        FakeSpaceProbe world = new FakeSpaceProbe(new BlockPos(-300, 0, -300), new BlockPos(300, 100, 300));
        RandomGenerator random = seeded(seed);
        Steering steering = Steering.around(MIDDLE_SPAWN, random);
        CourseGenerator generator = new CourseGenerator(world, MIDDLE_SPAWN, random, PortalClearance.NONE, steering);
        CourseBlock start = TestBlocks.at(new BlockPos(30, 10, 0), Surface.FULL);
        return walk(generator, start, new Phase.Scored(0, EAST), SNAKE_POINTS);
    }

    private static double distanceToSpawn(CourseBlock block) {
        return Math.hypot(block.pos().x() + 0.5 - MIDDLE_SPAWN.x(), block.pos().z() + 0.5 - MIDDLE_SPAWN.z());
    }

    /**
     * The side each step turns to relative to the step before; straight steps count for nothing.
     */
    private static List<Integer> turningSides(List<CourseBlock> course) {
        List<Integer> sides = new ArrayList<>();
        for (int i = 2; i < course.size(); i++) {
            Direction before = new Jump(course.get(i - 2), course.get(i - 1)).direction();
            Direction after = new Jump(course.get(i - 1), course.get(i)).direction();
            double cross = (double) before.dx() * after.dz() - (double) before.dz() * after.dx();
            if (cross != 0.0) {
                sides.add((int) Math.signum(cross));
            }
        }
        return sides;
    }

    @Test
    void theSnakeRunsAllSixtyPoints() {
        assertEquals(SNAKE_POINTS + 1, snake(SNAKE_SEED).size(), "start plus sixty blocks");
    }

    @Test
    void theSnakeCurvesToBothSides() {
        List<Integer> sides = turningSides(snake(SNAKE_SEED));

        long changes = IntStream.range(1, sides.size()).filter(i -> !sides.get(i).equals(sides.get(i - 1))).count();
        assertTrue(changes >= 4, "the turning side changes several times, got " + changes);
    }

    @Test
    void theSnakeComesBackTowardsTheSpawn() {
        double farthest = 0.0;
        double biggestDrop = 0.0;
        for (CourseBlock block : snake(SNAKE_SEED)) {
            farthest = Math.max(farthest, distanceToSpawn(block));
            biggestDrop = Math.max(biggestDrop, farthest - distanceToSpawn(block));
        }

        assertTrue(biggestDrop >= 8.0, "the distance falls at least 8 blocks below an earlier maximum, got " + biggestDrop);
    }

    @Test
    void theSnakeStaysInTheRingOnAverage() {
        double mean = snake(SNAKE_SEED).stream().mapToDouble(CourseGeneratorTest::distanceToSpawn).average().orElseThrow();

        assertTrue(mean >= Steering.Around.RING_MIN && mean <= Steering.Around.RING_MAX, "mean distance " + mean);
    }

    @Test
    void theSnakeNeverComesCloserToTheSpawnThanSixteenBlocks() {
        for (CourseBlock block : snake(SNAKE_SEED)) {
            assertTrue(distanceToSpawn(block) >= SpawnZone.MIN_SPAWN_DISTANCE, "block " + block.pos());
        }
    }

    @Test
    void theSnakeIsTheSameForTheSameSeed() {
        assertEquals(snake(SNAKE_SEED), snake(SNAKE_SEED), "same seed, same snake");
    }

    // --- randomness -----------------------------------------------------------------------------

    @Test
    void sameSeedGivesTheSameCourse() {
        List<CourseBlock> first = walk(new CourseGenerator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, seeded(42L)), SOURCE, new Phase.Scored(0, EAST), 50);
        List<CourseBlock> second = walk(new CourseGenerator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, seeded(42L)), SOURCE, new Phase.Scored(0, EAST), 50);

        assertEquals(first, second, "same seed, same course");
    }

    @Test
    void differentSeedsGiveDifferentCourses() {
        List<CourseBlock> first = walk(new CourseGenerator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, seeded(1L)), SOURCE, new Phase.Scored(0, EAST), 50);
        List<CourseBlock> second = walk(new CourseGenerator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, seeded(2L)), SOURCE, new Phase.Scored(0, EAST), 50);

        assertNotEquals(first, second, "different seeds, different courses");
    }

    // --- materials ------------------------------------------------------------------------------

    private static List<Block> fullBlockMaterials(long seed) {
        List<CourseBlock> course = walk(new CourseGenerator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, seeded(seed)), SOURCE, new Phase.Scored(0, EAST), 40);
        return course.stream().skip(1).filter(block -> block.surface() == Surface.FULL).limit(10).map(CourseBlock::material).toList();
    }

    @Test
    void tenFullBlocksInARowShowMoreThanOneMaterial() {
        List<Block> materials = fullBlockMaterials(5L);

        assertEquals(10, materials.size(), "ten full blocks within forty jumps");
        assertTrue(Set.copyOf(materials).size() > 1, "materials must vary, got " + materials);
    }

    @Test
    void sameSeedGivesTheSameMaterials() {
        assertEquals(fullBlockMaterials(5L), fullBlockMaterials(5L), "same seed, same materials");
    }

    @Test
    void everyGeneratedBlockUsesAMaterialOfItsOwnSurface() {
        List<CourseBlock> course = walk(new CourseGenerator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, seeded(9L)), SOURCE, new Phase.Scored(80, EAST), 200);

        for (CourseBlock block : course.subList(1, course.size())) {
            assertTrue(block.surface().palette().contains(block.material()), block.material() + " is not a material of " + block.surface());
        }
    }

    // --- statistics -----------------------------------------------------------------------------

    private static List<Jump> thousandJumpsAtScore(int score, long seed) {
        CourseGenerator generator = new CourseGenerator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, seeded(seed));
        List<Jump> jumps = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            jumps.add(new Jump(SOURCE, jumpFromSource(generator, score)));
        }
        return jumps;
    }

    private static double meanCost(List<Jump> jumps) {
        return jumps.stream().mapToDouble(Jump::cost).average().orElseThrow();
    }

    @Test
    void theStartIsMostlyFullBlocksWithShortGaps() {
        List<Jump> jumps = thousandJumpsAtScore(0, 100L);

        long easy = jumps.stream().filter(jump -> jump.to().surface() == Surface.FULL && jump.gap() <= 2).count();

        assertTrue(easy > 800, "over 80% easy jumps at score 0, got " + easy + " of 1000");
    }

    @Test
    void belowScoreTenOnlyFullBlocksAreChosen() {
        for (int score : new int[]{0, 9}) {
            long narrow = thousandJumpsAtScore(score, 100L).stream().filter(jump -> jump.to().surface() != Surface.FULL).count();

            assertEquals(0, narrow, "narrow shapes are locked at score " + score);
        }
    }

    @Test
    void slabsAndTrapdoorsAppearOnceTheyAreUnlockedAtScoreTen() {
        long unlocked = thousandJumpsAtScore(10, 100L).stream().filter(jump -> jump.to().surface() == Surface.SLAB || jump.to().surface() == Surface.TRAPDOOR).count();

        assertTrue(unlocked > 0, "slabs and trapdoors at score 10");
    }

    @Test
    void jumpsAtScoreEightyAreMuchHarderThanAtScoreZero() {
        double atZero = meanCost(thousandJumpsAtScore(0, 100L));
        double atEighty = meanCost(thousandJumpsAtScore(80, 100L));

        assertTrue(atEighty > atZero + 4.0, "mean cost " + atZero + " at 0 vs " + atEighty + " at 80");
    }

    @Test
    void jumpsAtScoreEightyUseNarrowSurfacesMoreOften() {
        long narrowAtZero = thousandJumpsAtScore(0, 100L).stream().filter(j -> j.to().surface() != Surface.FULL).count();
        long narrowAtEighty = thousandJumpsAtScore(80, 100L).stream().filter(j -> j.to().surface() != Surface.FULL).count();

        assertTrue(narrowAtEighty > narrowAtZero + 300, "narrow surfaces " + narrowAtZero + " vs " + narrowAtEighty);
    }

    // --- property: no impossible jump -----------------------------------------------------------

    private static final int SEEDS = 10;
    private static final int JUMPS_PER_SEED = 1000;

    private static FakeSpaceProbe obstacleWorld() {
        return new FakeSpaceProbe().occupyBox(-50, 0, -50, 50, 2, 50).occupyBox(-20, 10, 15, 20, 40, 18).occupyBox(30, 10, -50, 33, 60, 50);
    }

    /** Generated once for all properties; immutable, so sharing it keeps the tests independent. */
    private List<List<Move>> courses;
    private List<Jump> jumps;
    private FakeSpaceProbe world;

    @BeforeAll
    void generateJumps() {
        world = obstacleWorld();
        courses = generatedCourses(world);
        jumps = courses.stream().flatMap(List::stream).map(Move::jump).toList();
    }

    /** Ten seeded courses of 1000 jumps each. */
    private static List<List<Move>> generatedCourses(FakeSpaceProbe world) {
        List<List<Move>> courses = new ArrayList<>();
        for (long seed = 1; seed <= SEEDS; seed++) {
            courses.add(List.copyOf(moves(new CourseGenerator(world, TestBlocks.FAR_SPAWN, seeded(seed)), SOURCE, new Phase.Scored(0, EAST), JUMPS_PER_SEED)));
        }
        return List.copyOf(courses);
    }

    private static void assertForEvery(List<Jump> jumps, Predicate<Jump> property, String what) {
        for (Jump jump : jumps) {
            assertTrue(property.test(jump), what + ": " + jump);
        }
    }

    private static final long MAX_PROBE_CALLS_PER_NEXT = 20_000;

    /** Probe questions of the dearest single {@code next()} over a walk through the world. */
    private static long mostProbeCallsPerNext(FakeSpaceProbe world, long seed, int jumps) {
        CountingProbe counting = new CountingProbe(world);
        CourseGenerator generator = new CourseGenerator(counting, TestBlocks.FAR_SPAWN, seeded(seed));
        List<CourseBlock> course = new ArrayList<>(List.of(SOURCE));
        Phase phase = new Phase.Scored(0, EAST);
        long most = 0;
        for (int i = 0; i < jumps; i++) {
            long before = counting.calls();
            Optional<CourseBlock> next = generator.next(course, phase);
            most = Math.max(most, counting.calls() - before);
            if (next.isEmpty()) {
                break;
            }
            course.add(next.get());
            phase = generator.after(course, phase);
        }
        return most;
    }

    @Test
    void oneNextCallAsksTheWorldAtMostTwentyThousandTimes() {
        // The dead-end check runs on the tick thread in the move handler; counting probe calls
        // bounds its work without a wall clock.
        long worst = 0;
        for (long seed = 1; seed <= SEEDS; seed++) {
            worst = Math.max(worst, mostProbeCallsPerNext(world, seed, 300));
        }

        assertTrue(worst <= MAX_PROBE_CALLS_PER_NEXT, "one next() asked the world " + worst + " times");
    }

    @Test
    void mostSeedsCompleteTheirJumpsInTheObstacleWorld() {
        // With the main heading (D15) a course cannot turn back, so in this small walled world it
        // may run into a corner; that must stay the exception, not the rule. The depth-4 dead-end
        // lookahead (D15) is what keeps it rare.
        long complete = courses.stream().filter(course -> course.size() == JUMPS_PER_SEED).count();

        assertTrue(complete >= SEEDS * 8 / 10, complete + " of " + SEEDS + " courses completed their " + JUMPS_PER_SEED + " jumps");
    }

    @Test
    void everyCourseThatEndsEarlyHasStillMadeHundredsOfJumps() {
        for (List<Move> course : courses) {
            assertTrue(course.size() >= 200, "a course ended after only " + course.size() + " jumps");
        }
    }

    @Test
    void generatedGapsStayWithinTheLimits() {
        assertForEvery(jumps, jump -> jump.gap() >= Jump.MIN_GAP && jump.gap() <= Jump.MAX_GAP, "gap");
    }

    @Test
    void generatedAscentsStayWithinTheAscentGap() {
        assertForEvery(jumps, jump -> !jump.isAscent() || jump.gap() <= Jump.MAX_GAP_ASCENT, "ascent gap");
    }

    @Test
    void generatedDiagonalJumpsStayWithinTheDiagonalGap() {
        assertForEvery(jumps, jump -> !jump.isDiagonal() || jump.gap() <= Jump.MAX_GAP_DIAGONAL, "diagonal gap");
    }

    @Test
    void generatedRisesStayWithinTheLimit() {
        assertForEvery(jumps, jump -> jump.rise() <= Jump.MAX_RISE, "rise");
    }

    @Test
    void generatedJumpsAreFreeInTheWorld() {
        assertForEvery(jumps, new JumpRules(world)::isValid, "rules");
    }

    @Test
    void generatedBlocksHaveSixAirBlocksBelowThem() {
        assertForEvery(jumps, jump -> new Openness(world).hasAirBelow(jump.to().pos(), Openness.SCORED_AIR_BELOW), "air below");
    }

    @Test
    void generatedBlocksKeepTheMarginToTheTop() {
        assertForEvery(jumps, jump -> jump.to().pos().y() + JumpRules.MAX_Y_MARGIN <= 100, "top margin");
    }

    @Test
    void noGeneratedStepLeadsAgainstTheMainHeading() {
        for (List<Move> course : courses) {
            for (Move move : course) {
                double cosine = move.heading().dot(move.jump().direction());
                assertTrue(cosine >= 0.0, "step " + move.jump().direction() + " against the heading " + move.heading());
            }
        }
    }

    @Test
    void everyGeneratedTargetKeepsTwoCellsFromTheThreeBlocksBeforeItsSource() {
        for (List<Move> course : courses) {
            for (int i = 0; i < course.size(); i++) {
                BlockPos target = course.get(i).jump().to().pos();
                for (int back = 1; back <= 3 && back <= i; back++) {
                    BlockPos earlier = course.get(i - back).jump().from().pos();
                    int sourceY = course.get(i).jump().from().pos().y();
                    boolean inHeightSpan = earlier.y() >= Math.min(sourceY, target.y()) - Clearance.HEIGHT_MARGIN && earlier.y() <= Math.max(sourceY, target.y()) + Clearance.HEIGHT_MARGIN;
                    int distance = Math.max(Math.abs(earlier.x() - target.x()), Math.abs(earlier.z() - target.z()));
                    assertTrue(!inHeightSpan || distance >= Clearance.MIN_DISTANCE, "block " + (i + 1) + " at " + target + " is " + distance + " from the earlier " + earlier);
                }
            }
        }
    }

    @Test
    void everyGeneratedFlightPathKeepsClearanceToTheEarlierVisibleBlocks() {
        for (List<Move> course : courses) {
            List<Placement> blocks = new ArrayList<>(List.of(SOURCE));
            for (Move move : course) {
                List<Placement> visible = blocks.subList(Math.max(0, blocks.size() - Course.VISIBLE_BEFORE_NEW), blocks.size());
                assertTrue(Clearance.isKept(move.jump(), visible), "clearance: " + move.jump());
                blocks.add(move.jump().to());
            }
        }
    }

    @Test
    void everyGeneratedShapeIsUnlockedAtTheScoreOfItsJump() {
        for (List<Move> course : courses) {
            for (int score = 0; score < course.size(); score++) {
                Surface surface = course.get(score).jump().to().surface();
                assertTrue(surface.minScore() <= score, surface + " appeared at score " + score);
            }
        }
    }
}
