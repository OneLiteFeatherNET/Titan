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
package net.onelitefeather.titan.feature.jumprun.space;

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
    void sixAirBlocksBelowAreEnoughForTheScoredMinimum() {
        assertTrue(in(new FakeSpaceProbe().occupy(0, 23, 0)).hasAirBelow(TARGET, Openness.SCORED_AIR_BELOW), "y-1 to y-6 are air, y-7 is not");
    }

    @Test
    void aBlockSixBelowIsNotEnoughForTheScoredMinimum() {
        assertFalse(in(new FakeSpaceProbe().occupy(0, 24, 0)).hasAirBelow(TARGET, Openness.SCORED_AIR_BELOW), "y-6");
    }

    @Test
    void aBlockDirectlyBelowIsNotEnough() {
        assertFalse(in(new FakeSpaceProbe().occupy(0, 29, 0)).hasAirBelow(TARGET, Openness.SCORED_AIR_BELOW), "y-1");
    }

    @Test
    void sixAirBlocksBelowAreNotEnoughForTheAscent() {
        Openness openness = in(new FakeSpaceProbe().occupy(0, 23, 0));

        assertFalse(openness.hasAirBelow(TARGET, Openness.ASCENT_AIR_BELOW), "eight air blocks are needed, only six are there");
    }

    @Test
    void blocksBesideTheColumnDoNotMatterForTheAirBelow() {
        assertTrue(in(new FakeSpaceProbe().occupyBox(1, 20, -1, 1, 40, 1)).hasAirBelow(TARGET, Openness.SCORED_AIR_BELOW), "a wall next to the column");
    }

    @Test
    void cellsBelowTheWorldBottomAreNoOpenAir() {
        assertFalse(in(new FakeSpaceProbe()).hasAirBelow(new BlockPos(0, 5, 0), Openness.SCORED_AIR_BELOW), "y-6 is below the world");
    }

    @Test
    void theLowestBlockWithAllCellsInTheWorldBelowItHasAirBelow() {
        assertTrue(in(new FakeSpaceProbe()).hasAirBelow(new BlockPos(0, 6, 0), Openness.SCORED_AIR_BELOW), "y-1 to y-6 are y=5 to y=0");
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
    void groundEightBlocksDeepTakesHalfOfTheColumnShare() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(0, 22, 0, 0, 29, 0);

        assertEquals(0.7 * 8.0 / 16.0 + 0.3, in(world).of(TARGET), 1e-9, "half the column is taken, the neighbours are open");
    }

    @Test
    void theColumnIsLookedAtSixteenBlocksDown() {
        assertEquals(0.7 * 15.0 / 16.0 + 0.3, in(new FakeSpaceProbe().occupy(0, 14, 0)).of(TARGET), 1e-9, "y-16 counts");
        assertEquals(1.0, in(new FakeSpaceProbe().occupy(0, 13, 0)).of(TARGET), 1e-9, "y-17 does not");
    }

    @Test
    void aWallBesideTheTargetCostsOnlyThePartOfTheNeighbours() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(1, 0, -5, 1, 60, 5);

        assertEquals(0.7 + 0.3 * 10.0 / 16.0, in(world).of(TARGET), 1e-9, "three neighbour cells at y and three at y+1 are taken");
    }

    @Test
    void aTakenColumnWeighsMoreThanTakenNeighbours() {
        double walledIn = in(new FakeSpaceProbe().occupyBox(1, 0, -5, 1, 60, 5)).of(TARGET);
        double overAWay = in(new FakeSpaceProbe().occupyBox(-5, 14, -5, 5, 29, 5)).of(TARGET);

        assertTrue(overAWay < walledIn, "air below is what counts: " + overAWay + " vs " + walledIn);
    }

    @Test
    void neighboursAboveTheFeetDoNotCount() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(1, 32, -1, 1, 40, 1);

        assertEquals(1.0, in(world).of(TARGET), 1e-9, "head height is not looked at");
    }

    @Test
    void cellsBeyondTheBorderCountAsClosed() {
        BlockPos atBorder = new BlockPos(50, 30, 0);

        assertEquals(0.7 + 0.3 * 10.0 / 16.0, in(new FakeSpaceProbe()).of(atBorder), 1e-9, "three neighbour cells at y and three at y+1 lie outside");
    }

    @Test
    void cellsBelowTheWorldBottomCountAsClosed() {
        assertEquals(0.7 * 8.0 / 16.0 + 0.3, in(new FakeSpaceProbe()).of(new BlockPos(0, 8, 0)), 1e-9, "eight of the sixteen cells below lie under y=0");
    }
}
