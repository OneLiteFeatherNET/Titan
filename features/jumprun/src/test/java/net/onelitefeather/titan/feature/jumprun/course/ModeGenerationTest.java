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

import static net.onelitefeather.titan.feature.jumprun.course.ModeWalks.Move;
import static net.onelitefeather.titan.feature.jumprun.course.ModeWalks.walk;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;
import net.onelitefeather.titan.feature.jumprun.space.FlightPath;
import org.junit.jupiter.api.Test;

/**
 * The mode checks on the PR set: single-seed walks plus the seed-loop checks with a few seeds. The
 * same checks over 12, 20 and 200 seeds run in {@link ModeGenerationSweepTest} (tag {@code sweep}).
 */
class ModeGenerationTest {

    /** Seeds per loop check on the PR set; {@link ModeGenerationSweepTest} uses the full ranges. */
    private static final long PR_SEEDS = 2;

    @Test
    void aNewBlockNeverLandsNearTheJumpThatIsStillToBeMade() {
        for (Mode mode : Mode.values()) {
            List<Move> moves = walk(mode, 7L);
            for (int i = 1; i < moves.size(); i++) {
                Jump pending = moves.get(i - 1).jump();
                BlockPos target = moves.get(i).jump().to().pos();
                int low = Math.min(pending.from().pos().y(), pending.to().pos().y()) - Clearance.HEIGHT_MARGIN;
                int high = Math.max(pending.from().pos().y(), pending.to().pos().y()) + Clearance.HEIGHT_MARGIN;
                boolean inSpan = target.y() >= low && target.y() <= high;
                boolean near = FlightPath.cellsBetween(pending.from().pos(), pending.to().pos()).stream().anyMatch(cell -> Math.max(Math.abs(cell.x() - target.x()), Math.abs(cell.z() - target.z())) < Clearance.MIN_DISTANCE);
                assertTrue(!(inSpan && near), mode + " put " + target + " next to the pending jump " + pending.from().pos() + " to " + pending.to().pos());
            }
        }
    }

    @Test
    void easyOnlyEverMakesFullBlocksAndSlabsBeyondSixtyPoints() {
        List<Move> moves = walk(Mode.EASY, 1L);

        assertTrue(moves.size() > 60, "the walk goes past sixty points, got " + moves.size());
        for (Move move : moves) {
            Surface surface = move.jump().to().surface();
            assertTrue(surface == Surface.FULL || surface == Surface.SLAB, "easy made " + surface + " at score " + move.score());
        }
    }

    @Test
    void easyNeverLeavesAGapOfMoreThanTwo() {
        for (Move move : walk(Mode.EASY, 1L)) {
            assertTrue(move.jump().gap() <= 2, "gap " + move.jump().gap() + " at score " + move.score());
        }
    }

    @Test
    void easyMakesSlabsOnceTheyAreUnlocked() {
        assertTrue(walk(Mode.EASY, 1L).stream().anyMatch(move -> move.jump().to().surface() == Surface.SLAB), "slabs appear in easy");
    }

    @Test
    void hardMakesNoShapeBeforeItsThreshold() {
        for (Move move : walk(Mode.HARD, 2L)) {
            Surface surface = move.jump().to().surface();
            assertTrue(Mode.HARD.minScore(surface) <= move.score(), surface + " appeared at score " + move.score());
        }
    }

    @Test
    void hardReachesPostsWithinTheWalk() {
        assertTrue(walk(Mode.HARD, 2L).stream().anyMatch(move -> move.jump().to().surface() == Surface.POST), "posts appear in hard");
    }

    @Test
    void mediumNeverMakesANarrowSurface() {
        ModeWalkChecks.assertMediumNeverMakesANarrowSurface(PR_SEEDS);
    }

    @Test
    void hardMakesNarrowSurfacesWithinTheWalk() {
        ModeWalkChecks.assertHardMakesNarrowSurfacesWithinTheWalk(PR_SEEDS);
    }

    @Test
    void mediumReachesTheShapesWithAWideTopFaceWithinTheWalk() {
        ModeWalkChecks.assertMediumReachesTheShapesWithAWideTopFaceWithinTheWalk(PR_SEEDS);
    }

    @Test
    void mediumIsReproducibleFromTheSameSeed() {
        assertEquals(walk(Mode.MEDIUM, 5L), walk(Mode.MEDIUM, 5L), "same seed, same course");
    }

    @Test
    void hardIsHarderThanEasyOnAverage() {
        double easy = meanCostFrom(Mode.EASY, 40);
        double hard = meanCostFrom(Mode.HARD, 40);

        assertTrue(hard > easy, "hard " + hard + " must cost more than easy " + easy);
    }

    @Test
    void theHardestPossibleJumpStaysTheSameWithTheNewShapes() {
        assertEquals(2.0 * 4 + 1.5 * 3, Jump.MAX_COST, "post over the widest flat gap");
    }

    @Test
    void everyStairsBlockOfAWalkHasAFacingAndEveryHeadATurn() {
        for (Move move : walk(Mode.MEDIUM, 3L)) {
            CourseBlock block = (CourseBlock) move.jump().to();
            if (block.surface() == Surface.STAIRS) {
                assertTrue(List.of("north", "south", "east", "west").contains(block.material().getProperty("facing")), "stairs facing at score " + move.score());
            }
            if (block.surface() == Surface.HEAD) {
                int turn = Integer.parseInt(block.material().getProperty("rotation"));
                assertTrue(turn >= 0 && turn <= 15, "head turn " + turn);
            }
        }
    }

    @Test
    void easyMakesNoNewShape() {
        ModeWalkChecks.assertEasyMakesNoNewShape(PR_SEEDS);
    }

    @Test
    void noJumpOverstepsTheLimitsOfGapAndRiseWhateverTheShape() {
        ModeWalkChecks.assertNoJumpOverstepsTheLimitsOfGapAndRiseWhateverTheShape(PR_SEEDS);
    }

    @Test
    void easyMakesNoTowerAtAnyScore() {
        ModeWalkChecks.assertEasyMakesNoTowerAtAnyScore(PR_SEEDS);
    }

    @Test
    void mediumMakesNoTowerBeforeScore30() {
        ModeWalkChecks.assertMediumMakesNoTowerBeforeScore30(PR_SEEDS);
    }

    @Test
    void mediumMakesTowersFromScore30() {
        ModeWalkChecks.assertMediumMakesTowersFromScore30(PR_SEEDS);
    }

    @Test
    void mediumMakesTowersAtScore80() {
        ModeWalkChecks.assertMediumMakesTowersAtScore80(PR_SEEDS);
    }

    @Test
    void hardMakesTheTallestTowerWithTheDefaultLimitsAtScoreEighty() {
        ModeWalkChecks.assertHardMakesTheTallestTowerWithTheDefaultLimitsAtScoreEighty(PR_SEEDS);
    }

    @Test
    void everyTowerIsStraightAndWithinTheDefaultHeights() {
        ModeWalkChecks.assertEveryTowerIsStraightAndWithinTheDefaultHeights(PR_SEEDS);
    }

    @Test
    void noJumpCostsMoreThanTheMaximumWithTowers() {
        ModeWalkChecks.assertNoJumpCostsMoreThanTheMaximumWithTowers(PR_SEEDS);
    }

    private static double meanCostFrom(Mode mode, int fromScore) {
        return walk(mode, 7L).stream().filter(move -> move.score() >= fromScore).mapToDouble(move -> move.jump().cost(mode)).average().orElseThrow();
    }
}
