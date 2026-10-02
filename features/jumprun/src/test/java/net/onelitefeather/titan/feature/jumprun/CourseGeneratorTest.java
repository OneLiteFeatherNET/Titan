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
import java.util.Set;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CourseGeneratorTest {

    private static final CourseBlock SOURCE = new CourseBlock(new BlockPos(0, 10, 0), Surface.FULL);

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
        return generator.next(List.of(SOURCE), new Phase.Scored(score)).orElseThrow();
    }

    /** Walks the generator forward from the start and returns the whole course, start included. */
    private static List<CourseBlock> walk(CourseGenerator generator, CourseBlock start, Phase phase, int jumps) {
        List<CourseBlock> course = new ArrayList<>(List.of(start));
        for (int i = 0; i < jumps; i++) {
            Optional<CourseBlock> next = generator.next(course, phase);
            if (next.isEmpty()) {
                break;
            }
            course.add(next.get());
            phase = generator.after(next.get(), phase);
        }
        return course;
    }

    // --- choosing by cost -----------------------------------------------------------------------

    @Test
    void picksTheCandidateWithTheTargetCostWhenTheTargetIsZero() {
        CourseGenerator generator = new CourseGenerator(new FakeSpaceProbe(), withNoise(-100.0, 1L));

        CourseBlock chosen = jumpFromSource(generator, 0);

        assertEquals(0.0, new Jump(SOURCE, chosen).cost(), "an easiest jump");
    }

    @Test
    void picksTheCandidateClosestToTheTargetCost() {
        CourseGenerator generator = new CourseGenerator(new FakeSpaceProbe(), withNoise(2.0, 1L));

        CourseBlock chosen = jumpFromSource(generator, 0);

        assertEquals(2.0, new Jump(SOURCE, chosen).cost(), "target 2.0 is met exactly by a slab");
        assertEquals(Surface.SLAB, chosen.surface(), "only a slab costs 2.0 without a gap or a rise");
    }

    @Test
    void picksTheHardestJumpWhenTheTargetIsAboveEverything() {
        CourseGenerator generator = new CourseGenerator(new FakeSpaceProbe(), withNoise(100.0, 1L));

        CourseBlock chosen = jumpFromSource(generator, 10_000);

        assertEquals(Jump.MAX_COST, new Jump(SOURCE, chosen).cost(), "hardest allowed jump");
    }

    // --- space ----------------------------------------------------------------------------------

    @Test
    void rejectsACandidateWithoutAnyFollowUpJump() {
        for (long seed = 0; seed < 20; seed++) {
            // A is the easiest candidate but nothing is reachable from it; B has a follow-up C.
            FakeSpaceProbe world = FakeSpaceProbe.solidWorld().carveColumn(2, 5, 0, 10).carveColumn(1, 11, 0, 2).carveColumn(-5, 5, 0, 10).carveColumn(-10, 5, 0, 10);
            for (int x = -9; x <= -1; x++) {
                world.carveColumn(x, 11, 0, 2);
            }
            CourseGenerator generator = new CourseGenerator(world, withNoise(0.0, seed));

            CourseBlock chosen = jumpFromSource(generator, 0);

            assertTrue(chosen.pos().x() < 0, "dead end A in the east must lose to the way west (seed " + seed + "), chose " + chosen.pos());
        }
    }

    @Test
    void neverPlacesAScoredBlockOverAWay() {
        // From y=10 a block reaches at most y=11, which leaves no four air blocks above the way at y<=7.
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(0, 0, -50, 50, 7, 50);
        for (long seed = 0; seed < 30; seed++) {
            CourseBlock chosen = jumpFromSource(new CourseGenerator(world, seeded(seed)), 0);

            assertTrue(chosen.pos().x() < 0, "only the open side may be used (seed " + seed + "), chose " + chosen.pos());
        }
    }

    @Test
    void reportsNoCandidateWhenEveryPlaceIsOverAWay() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(-50, 0, -50, 50, 7, 50);

        assertTrue(new CourseGenerator(world, seeded(1L)).next(List.of(SOURCE), new Phase.Scored(0)).isEmpty(), "ground three blocks below the start leaves no room");
    }

    @Test
    void prefersTheMoreOpenOfTwoEquallyCostlyPlaces() {
        // East and west offer the same four zero-cost jumps; a wall hugs the west ones.
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(0, 8, 2, 0, 12, 2).occupyBox(0, 8, -2, 0, 12, -2).occupyBox(-3, 0, -50, -3, 100, 50);
        for (long seed = 0; seed < 30; seed++) {
            CourseBlock chosen = jumpFromSource(new CourseGenerator(world, withNoise(-100.0, seed)), 0);

            assertTrue(chosen.pos().x() > 0, "the open side wins at equal cost (seed " + seed + "), chose " + chosen.pos());
        }
    }

    @Test
    void neverPlacesABlockIntoTheHeadroomOfAnEarlierBlock() {
        CourseBlock earlier = new CourseBlock(new BlockPos(0, 10, 0), Surface.FULL);
        CourseBlock last = new CourseBlock(new BlockPos(2, 10, 0), Surface.FULL);
        Phase towardsTheEarlierBlock = new Phase.Ascent(Phase.MIN_ASCENT_JUMPS, new Heading(-1.0, 0.0));

        for (long seed = 0; seed < 200; seed++) {
            CourseGenerator generator = new CourseGenerator(new FakeSpaceProbe(), seeded(seed));

            BlockPos chosen = generator.next(List.of(earlier, last), towardsTheEarlierBlock).orElseThrow().pos();

            boolean inItsColumn = chosen.x() == 0 && chosen.z() == 0 && chosen.y() <= earlier.headroomTopY();
            assertFalse(inItsColumn, "inside the earlier block or its headroom: " + chosen + " (seed " + seed + ")");
        }
    }

    @Test
    void reportsNoCandidateWhenNothingIsFree() {
        CourseGenerator generator = new CourseGenerator(FakeSpaceProbe.solidWorld(), seeded(1L));

        assertTrue(generator.next(List.of(SOURCE), new Phase.Scored(0)).isEmpty(), "walled in");
    }

    @Test
    void leadsAroundAWallInsteadOfThroughIt() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(6, 0, -50, 50, 100, 50);
        CourseGenerator generator = new CourseGenerator(world, seeded(3L));

        List<CourseBlock> course = walk(generator, SOURCE, new Phase.Scored(0), 60);

        assertTrue(course.size() > 40, "the course must keep going, got " + course.size());
        for (CourseBlock block : course) {
            assertTrue(block.pos().x() < 6, "block inside the wall: " + block.pos());
        }
    }

    @Test
    void doesNotRepeatAPositionOfTheLastFourBlocks() {
        CourseGenerator generator = new CourseGenerator(new FakeSpaceProbe(), seeded(11L));

        List<CourseBlock> course = walk(generator, SOURCE, new Phase.Scored(0), 300);

        for (int i = 1; i < course.size(); i++) {
            for (int back = 1; back <= 4 && back <= i; back++) {
                assertNotEquals(course.get(i - back).pos(), course.get(i).pos(), "block " + i + " repeats the one " + back + " before it");
            }
        }
    }

    // --- randomness -----------------------------------------------------------------------------

    @Test
    void sameSeedGivesTheSameCourse() {
        List<CourseBlock> first = walk(new CourseGenerator(new FakeSpaceProbe(), seeded(42L)), SOURCE, new Phase.Scored(0), 50);
        List<CourseBlock> second = walk(new CourseGenerator(new FakeSpaceProbe(), seeded(42L)), SOURCE, new Phase.Scored(0), 50);

        assertEquals(first, second, "same seed, same course");
    }

    @Test
    void differentSeedsGiveDifferentCourses() {
        List<CourseBlock> first = walk(new CourseGenerator(new FakeSpaceProbe(), seeded(1L)), SOURCE, new Phase.Scored(0), 50);
        List<CourseBlock> second = walk(new CourseGenerator(new FakeSpaceProbe(), seeded(2L)), SOURCE, new Phase.Scored(0), 50);

        assertNotEquals(first, second, "different seeds, different courses");
    }

    // --- materials ------------------------------------------------------------------------------

    private static List<Block> fullBlockMaterials(long seed) {
        List<CourseBlock> course = walk(new CourseGenerator(new FakeSpaceProbe(), seeded(seed)), SOURCE, new Phase.Scored(0), 40);
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
        List<CourseBlock> course = walk(new CourseGenerator(new FakeSpaceProbe(), seeded(9L)), SOURCE, new Phase.Scored(80), 200);

        for (CourseBlock block : course.subList(1, course.size())) {
            assertTrue(block.surface().palette().contains(block.material()), block.material() + " is not a material of " + block.surface());
        }
    }

    // --- statistics -----------------------------------------------------------------------------

    private static List<Jump> thousandJumpsAtScore(int score, long seed) {
        CourseGenerator generator = new CourseGenerator(new FakeSpaceProbe(), seeded(seed));
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
    private List<Jump> jumps;
    private FakeSpaceProbe world;

    @BeforeAll
    void generateJumps() {
        world = obstacleWorld();
        jumps = List.copyOf(generatedJumps(world));
    }

    /** The jumps of ten seeded courses of 1000 jumps each. */
    private static List<Jump> generatedJumps(FakeSpaceProbe world) {
        List<Jump> jumps = new ArrayList<>();
        for (long seed = 1; seed <= SEEDS; seed++) {
            List<CourseBlock> course = walk(new CourseGenerator(world, seeded(seed)), SOURCE, new Phase.Scored(0), JUMPS_PER_SEED);
            for (int i = 1; i < course.size(); i++) {
                jumps.add(new Jump(course.get(i - 1), course.get(i)));
            }
        }
        return jumps;
    }

    private static void assertForEvery(List<Jump> jumps, Predicate<Jump> property, String what) {
        for (Jump jump : jumps) {
            assertTrue(property.test(jump), what + ": " + jump);
        }
    }

    @Test
    void everySeedCompletesItsJumpsInTheObstacleWorld() {
        assertEquals(SEEDS * JUMPS_PER_SEED, jumps.size(), "generated jumps");
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
    void generatedBlocksHaveFourAirBlocksBelowThem() {
        assertForEvery(jumps, jump -> new Openness(world).hasAirBelow(jump.to().pos()), "air below");
    }

    @Test
    void generatedBlocksKeepTheMarginToTheTop() {
        assertForEvery(jumps, jump -> jump.to().pos().y() + JumpRules.MAX_Y_MARGIN <= 100, "top margin");
    }
}
