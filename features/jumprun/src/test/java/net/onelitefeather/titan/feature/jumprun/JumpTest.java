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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JumpTest {

    private static CourseBlock block(int x, int y, int z, Surface surface) {
        return new CourseBlock(new BlockPos(x, y, z), surface);
    }

    private static Jump jump(CourseBlock from, CourseBlock to) {
        return new Jump(from, to);
    }

    @Test
    void gapCountsTheAirBlocksBetweenTheBlocksAlongAnAxis() {
        assertEquals(1, jump(block(0, 0, 0, Surface.FULL), block(2, 0, 0, Surface.FULL)).gap(), "x axis");
        assertEquals(4, jump(block(0, 0, 0, Surface.FULL), block(0, 0, -5, Surface.FULL)).gap(), "z axis");
    }

    @Test
    void gapOfADiagonalStepUsesTheChebyshevDistance() {
        assertEquals(2, jump(block(0, 0, 0, Surface.FULL), block(3, 0, 3, Surface.FULL)).gap(), "diagonal");
        assertEquals(2, jump(block(0, 0, 0, Surface.FULL), block(3, 0, 1, Surface.FULL)).gap(), "knight like");
    }

    @Test
    void easiestJumpCostsNothing() {
        assertEquals(0.0, jump(block(0, 0, 0, Surface.FULL), block(2, 0, 0, Surface.FULL)).cost(), "full, gap 1, flat");
    }

    @Test
    void costGrowsByOneAndAHalfPerExtraGapBlock() {
        assertEquals(4.5, jump(block(0, 0, 0, Surface.FULL), block(5, 0, 0, Surface.FULL)).cost(), "gap 4");
    }

    @Test
    void costGrowsByTwoPerTypeStep() {
        assertEquals(2.0, jump(block(0, 0, 0, Surface.FULL), block(2, 0, 0, Surface.SLAB)).cost(), "slab");
        assertEquals(6.0, jump(block(0, 0, 0, Surface.FULL), block(2, -1, 0, Surface.PANE)).cost(), "pane, lower");
    }

    @Test
    void costGrowsByOneWhenTheTopRises() {
        assertEquals(1.0, jump(block(0, 0, 0, Surface.FULL), block(2, 1, 0, Surface.FULL)).cost(), "one block up");
    }

    @Test
    void aRisingTopCountsEvenWhenTheBlockPositionStaysLevel() {
        Jump fenceAfterFull = jump(block(0, 0, 0, Surface.FULL), block(2, 0, 0, Surface.FENCE));
        assertTrue(fenceAfterFull.isAscent(), "fence top is above a full block top");
        assertEquals(2 * 2 + 1.0, fenceAfterFull.cost(), "fence + ascent");
    }

    @Test
    void aLowerOrEqualTopIsNoAscent() {
        assertFalse(jump(block(0, 0, 0, Surface.FULL), block(2, 0, 0, Surface.PANE)).isAscent(), "equal top");
        assertFalse(jump(block(0, 0, 0, Surface.FENCE), block(2, 0, 0, Surface.FULL)).isAscent(), "lower top");
    }

    @Test
    void riseIsTheDifferenceBetweenTheTops() {
        assertEquals(2.0, jump(block(0, 0, 0, Surface.SLAB), block(2, 1, 0, Surface.FENCE)).rise(), "0.5 to 2.5");
    }

    @Test
    void maxCostIsTheHardestJumpOnTheFlat() {
        assertEquals(10.5, Jump.MAX_COST, "pane + gap 4");
    }
}
