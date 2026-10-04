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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ClearanceTest {

    private static CourseBlock block(int x, int y, int z) {
        return TestBlocks.at(new BlockPos(x, y, z), Surface.FULL);
    }

    private static final CourseBlock SOURCE = block(0, 10, 0);

    private static boolean kept(CourseBlock target, CourseBlock... visible) {
        return Clearance.isKept(new Jump(SOURCE, target), List.of(visible));
    }

    @Test
    void aJumpWithOnlyTheSourceVisibleKeepsItsClearance() {
        assertTrue(kept(block(2, 10, 0), SOURCE), "the source is excepted: the first flown cell lies right next to it");
    }

    @Test
    void aTargetNextToAnEarlierBlockIsTooClose() {
        assertFalse(kept(block(2, 10, 0), SOURCE, block(3, 10, 0)), "one cell from the earlier block");
    }

    @Test
    void aTargetTwoCellsFromAnEarlierBlockIsFarEnough() {
        assertTrue(kept(block(2, 10, 0), SOURCE, block(4, 10, 0)), "two cells apart");
    }

    @Test
    void aFlightPathPassingAnEarlierBlockIsTooClose() {
        assertFalse(kept(block(4, 10, 0), SOURCE, block(2, 10, 1)), "the path crosses beside the earlier block");
    }

    @Test
    void aFlightPathTwoCellsFromAnEarlierBlockIsFarEnough() {
        assertTrue(kept(block(4, 10, 0), SOURCE, block(2, 10, 2)), "two cells beside the path");
    }

    @Test
    void theDistanceIsMeasuredAsChebyshev() {
        assertFalse(kept(block(2, 10, 0), SOURCE, block(3, 10, 1)), "diagonal neighbour counts");
    }

    @Test
    void anEarlierBlockMoreThanTwoAboveTheJumpIsNotInTheWay() {
        assertTrue(kept(block(2, 10, 0), SOURCE, block(3, 13, 0)), "three above the jump's height span");
    }

    @Test
    void anEarlierBlockTwoAboveTheJumpIsInTheWay() {
        assertFalse(kept(block(2, 10, 0), SOURCE, block(3, 12, 0)), "two above the jump's height span");
    }

    @Test
    void anEarlierBlockMoreThanTwoBelowTheJumpIsNotInTheWay() {
        assertTrue(kept(block(2, 10, 0), SOURCE, block(3, 7, 0)), "three below");
    }

    private static boolean keptAfter(CourseBlock before, CourseBlock target) {
        return Clearance.isKept(new Jump(SOURCE, target), List.of(before, SOURCE));
    }

    @Test
    void aTargetBesideThePendingJumpIntoTheSourceIsTooClose() {
        assertFalse(keptAfter(block(-5, 10, 0), block(-2, 10, 1)), "one cell beside the cells the runner still flies over");
    }

    @Test
    void aTargetTwoCellsFromThePendingJumpIsFarEnough() {
        assertTrue(keptAfter(block(-5, 10, 0), block(0, 10, 2)), "two cells from the pending path");
    }

    @Test
    void aTargetOnThePathOfTheJumpTwoBeforeTheSourceIsTooClose() {
        CourseBlock first = block(3, 10, -6);
        CourseBlock second = block(3, 10, 0);
        CourseBlock target = block(2, 10, -3);
        assertFalse(Clearance.isKept(new Jump(SOURCE, target), List.of(first, second, SOURCE)), "two from both ends of the first jump, but beside the middle of its path");
    }

    @Test
    void aTargetFarAboveThePendingJumpIsNotInTheWay() {
        assertTrue(keptAfter(block(-5, 10, 0), block(-2, 14, 1)), "above the pending jump's height span");
    }

    @Test
    void theHeightSpanCoversBothEndsOfTheJump() {
        assertFalse(kept(block(2, 12, 0), SOURCE, block(3, 14, 0)), "two above the higher end");
    }
}
