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

import java.util.concurrent.atomic.AtomicInteger;
import net.onelitefeather.titan.core.module.LobbyHeightBounds;
import org.junit.jupiter.api.Test;

class HeightBandTest {

    private static HeightBand band(int min, int max) {
        return new HeightBand(TestBlocks.bounds(min, max));
    }

    private static boolean allows(HeightBand band, Surface surface, int y) {
        return band.allows(TestBlocks.at(new BlockPos(0, y, 0), surface));
    }

    // --- lower limit: top - 3 - 4 must stay above minHeight --------------------------------------

    @Test
    void fullBlockAtTheLowestAllowedYAboveMinus64IsInside() {
        assertTrue(allows(band(-64, 310), Surface.FULL, -57), "top -56, a fall to -63 is still above -64");
    }

    @Test
    void fullBlockOneBelowTheLowestAllowedYAboveMinus64IsOutside() {
        assertFalse(allows(band(-64, 310), Surface.FULL, -58), "top -57, the fall reaches -64");
    }

    @Test
    void fullBlockAtTheLowestAllowedYAboveZeroIsInside() {
        assertTrue(allows(band(0, 310), Surface.FULL, 7), "top 8, 8 - 7 = 1 > 0");
    }

    @Test
    void fullBlockOneBelowTheLowestAllowedYAboveZeroIsOutside() {
        assertFalse(allows(band(0, 310), Surface.FULL, 6), "top 7, 7 - 7 = 0 is not above 0");
    }

    @Test
    void slabNeedsTheSameYAsAFullBlockAtTheLowerLimit() {
        assertTrue(allows(band(0, 310), Surface.SLAB, 7), "top 7.5 > 7");
        assertFalse(allows(band(0, 310), Surface.SLAB, 6), "top 6.5 is below 7");
    }

    @Test
    void fenceMayStandOneLowerBecauseItsTopIsHigher() {
        assertTrue(allows(band(0, 310), Surface.FENCE, 6), "top 7.5 > 7");
        assertFalse(allows(band(0, 310), Surface.FENCE, 5), "top 6.5 is below 7");
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
        HeightBand band = new HeightBand(new LobbyHeightBounds() {
            @Override
            public int minHeight() {
                return -64;
            }

            @Override
            public int maxHeight() {
                return max.get();
            }
        });
        assertTrue(allows(band, Surface.FULL, 100), "inside while the limit is 310");

        max.set(100);

        assertFalse(allows(band, Surface.FULL, 100), "outside once the operator lowers the limit");
    }

    @Test
    void theFallAllowanceSharesTheCourseFallDistance() {
        assertEquals(3, Course.FALL_DISTANCE, "a fall of three ends the run");
        assertEquals(4, HeightBand.MAX_FALL_PER_TICK, "terminal velocity is about 3.92 blocks per tick");
    }
}
