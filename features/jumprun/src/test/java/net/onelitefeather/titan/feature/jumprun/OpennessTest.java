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

class OpennessTest {

    private static final BlockPos TARGET = new BlockPos(0, 30, 0);

    private static Openness in(FakeSpaceProbe world) {
        return new Openness(world);
    }

    // --- air below --------------------------------------------------------------------------------

    @Test
    void fourAirBlocksBelowAreEnough() {
        assertTrue(in(new FakeSpaceProbe().occupy(0, 25, 0)).hasAirBelow(TARGET), "y-1 to y-4 are air, y-5 is not");
    }

    @Test
    void aBlockDirectlyBelowIsNotEnough() {
        assertFalse(in(new FakeSpaceProbe().occupy(0, 29, 0)).hasAirBelow(TARGET), "y-1");
    }

    @Test
    void aBlockFourBelowIsNotEnough() {
        assertFalse(in(new FakeSpaceProbe().occupy(0, 26, 0)).hasAirBelow(TARGET), "y-4");
    }

    @Test
    void blocksBesideTheColumnDoNotMatterForTheAirBelow() {
        assertTrue(in(new FakeSpaceProbe().occupyBox(1, 20, -1, 1, 40, 1)).hasAirBelow(TARGET), "a wall next to the column");
    }

    @Test
    void cellsBelowTheWorldBottomAreNoOpenAir() {
        assertFalse(in(new FakeSpaceProbe()).hasAirBelow(new BlockPos(0, 3, 0)), "y-4 is below the world");
    }

    @Test
    void theLowestBlockWithFourCellsInTheWorldBelowItHasAirBelow() {
        assertTrue(in(new FakeSpaceProbe()).hasAirBelow(new BlockPos(0, 4, 0)), "y-1 to y-4 are y=3 to y=0");
    }

    // --- openness ---------------------------------------------------------------------------------

    @Test
    void anOpenWorldIsFullyOpen() {
        assertEquals(1.0, in(new FakeSpaceProbe()).of(TARGET), 1e-9, "openness");
    }

    @Test
    void aWorldWithoutAnyAirIsClosed() {
        assertEquals(0.0, in(FakeSpaceProbe.solidWorld()).of(TARGET), 1e-9, "openness");
    }

    @Test
    void groundTwelveBlocksDeepCountsAsTwelveOfTwentyEightCells() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(0, 18, 0, 0, 29, 0);

        assertEquals(16.0 / 28.0, in(world).of(TARGET), 1e-9, "the 12 cells below the target are taken");
    }

    @Test
    void groundBeyondTwelveBlocksIsNotLookedAt() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(0, 0, 0, 0, 17, 0);

        assertEquals(1.0, in(world).of(TARGET), 1e-9, "only the first 12 blocks below count");
    }

    @Test
    void aWallBesideTheTargetCountsItsEightNeighboursAtTargetAndFeetLevel() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(1, 0, -5, 1, 60, 5);

        assertEquals(22.0 / 28.0, in(world).of(TARGET), 1e-9, "three neighbour cells at y and three at y+1 are taken");
    }

    @Test
    void neighboursAboveTheFeetDoNotCount() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(1, 32, -1, 1, 40, 1);

        assertEquals(1.0, in(world).of(TARGET), 1e-9, "head height is not looked at");
    }

    @Test
    void cellsBeyondTheBorderCountAsClosed() {
        BlockPos atBorder = new BlockPos(50, 30, 0);

        assertEquals(22.0 / 28.0, in(new FakeSpaceProbe()).of(atBorder), 1e-9, "three neighbour cells at y and three at y+1 lie outside");
    }

    @Test
    void cellsBelowTheWorldBottomCountAsClosed() {
        assertEquals(24.0 / 28.0, in(new FakeSpaceProbe()).of(new BlockPos(0, 8, 0)), 1e-9, "four of the twelve cells below lie under y=0");
    }
}
