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

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class HeightBandTest {

    private static HeightBand band(int min, int max) {
        return new HeightBand(TestBlocks.bounds(min, max));
    }

    private static boolean allows(HeightBand band, Surface surface, int y) {
        return band.allows(TestBlocks.at(new BlockPos(0, y, 0), surface));
    }

    // --- lower limit: top - FALL_DISTANCE - FALL_ALLOWANCE must stay above minHeight ---------------

    /**
     * One move packet after lag may report this much fall, a bit more than one tick at terminal
     * velocity.
     */
    private static final int FALL_ALLOWANCE = 5;

    /** The lowest y of a full block (top = y + 1) whose fall room stays above {@code min}. */
    private static int lowestFullY(int min) {
        return min + Course.FALL_DISTANCE + FALL_ALLOWANCE;
    }

    @Test
    void fullBlockAtTheLowestAllowedYAboveMinus64IsInside() {
        assertTrue(allows(band(-64, 310), Surface.FULL, lowestFullY(-64)), "the whole fall room stays above the limit");
    }

    @Test
    void fullBlockOneBelowTheLowestAllowedYAboveMinus64IsOutside() {
        assertFalse(allows(band(-64, 310), Surface.FULL, lowestFullY(-64) - 1), "the fall room reaches the limit");
    }

    @Test
    void fullBlockAtTheLowestAllowedYAboveZeroIsInside() {
        assertTrue(allows(band(0, 310), Surface.FULL, lowestFullY(0)), "the whole fall room stays above the limit");
    }

    @Test
    void fullBlockOneBelowTheLowestAllowedYAboveZeroIsOutside() {
        assertFalse(allows(band(0, 310), Surface.FULL, lowestFullY(0) - 1), "the fall room reaches the limit");
    }

    @Test
    void slabNeedsTheSameYAsAFullBlockAtTheLowerLimit() {
        assertTrue(allows(band(0, 310), Surface.SLAB, lowestFullY(0)), "top is half a block above the limit's reach");
        assertFalse(allows(band(0, 310), Surface.SLAB, lowestFullY(0) - 1), "top is half a block too low");
    }

    @Test
    void fenceMayStandOneLowerBecauseItsTopIsHigher() {
        assertTrue(allows(band(0, 310), Surface.FENCE, lowestFullY(0) - 1), "top is a half block higher than a full block's");
        assertFalse(allows(band(0, 310), Surface.FENCE, lowestFullY(0) - 2), "top is one block too low");
    }

    // --- upper limit: top + JUMP_HEIGHT + 1 must not exceed maxHeight -----------------------------

    @Test
    void fullBlockAtTheHighestAllowedYBelow310IsInside() {
        assertTrue(allows(band(-64, 310), Surface.FULL, 306), "top 307 + 2.2522 <= 310");
    }

    @Test
    void fullBlockOneAboveTheHighestAllowedYBelow310IsOutside() {
        assertFalse(allows(band(-64, 310), Surface.FULL, 307), "top 308 + 2.2522 > 310");
    }

    @Test
    void fullBlockAtTheHighestAllowedYBelow100IsInside() {
        assertTrue(allows(band(-64, 100), Surface.FULL, 96), "top 97 + 2.2522 <= 100");
    }

    @Test
    void fullBlockOneAboveTheHighestAllowedYBelow100IsOutside() {
        assertFalse(allows(band(-64, 100), Surface.FULL, 97), "top 98 + 2.2522 > 100");
    }

    @Test
    void slabMayStandOneHigherBecauseItsTopIsLower() {
        assertTrue(allows(band(-64, 100), Surface.SLAB, 97), "top 97.5 + 2.2522 <= 100");
        assertFalse(allows(band(-64, 100), Surface.SLAB, 98), "top 98.5 + 2.2522 > 100");
    }

    @Test
    void fenceNeedsTheSameYAsAFullBlockAtTheUpperLimit() {
        assertTrue(allows(band(-64, 100), Surface.FENCE, 96), "top 97.5 + 2.2522 <= 100");
        assertFalse(allows(band(-64, 100), Surface.FENCE, 97), "top 98.5 + 2.2522 > 100");
    }

    // --- live bounds ---------------------------------------------------------------------------

    @Test
    void theBoundsAreReadOnEveryCheckNotCachedAtCreation() {
        AtomicInteger max = new AtomicInteger(310);
        HeightBand band = new HeightBand(new TestBounds(() -> -64, max::get));
        assertTrue(allows(band, Surface.FULL, 100), "inside while the limit is 310");

        max.set(100);

        assertFalse(allows(band, Surface.FULL, 100), "outside once the operator lowers the limit");
    }
}
