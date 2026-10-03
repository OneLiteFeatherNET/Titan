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

import org.junit.jupiter.api.Test;

class JumpRulesTest {

    private static final CourseBlock ORIGIN = block(0, 10, 0, Surface.FULL);

    private static CourseBlock block(int x, int y, int z, Surface surface) {
        return TestBlocks.at(new BlockPos(x, y, z), surface);
    }

    /** A jump from the origin block to a block at the given offset, in an open world. */
    private static boolean valid(Surface to, int dx, int dy, int dz) {
        return validIn(new FakeSpaceProbe(), ORIGIN, block(dx, 10 + dy, dz, to));
    }

    private static boolean validIn(FakeSpaceProbe world, CourseBlock from, CourseBlock to) {
        return validIn(world, TestBlocks.BAND, from, to);
    }

    private static boolean validIn(FakeSpaceProbe world, HeightBand band, CourseBlock from, CourseBlock to) {
        return new JumpRules(world, band).isValid(new Jump(from, to), Mode.MEDIUM);
    }

    // --- reachability -------------------------------------------------------------------------

    @Test
    void flatJumpsWithGapOneToFourAreReachable() {
        for (int gap = 1; gap <= 4; gap++) {
            assertTrue(valid(Surface.FULL, gap + 1, 0, 0), "gap " + gap);
        }
    }

    @Test
    void flatJumpWithGapFiveIsTooFar() {
        assertFalse(valid(Surface.FULL, 6, 0, 0), "gap 5");
    }

    @Test
    void diagonalJumpWithGapTwoIsReachable() {
        assertTrue(valid(Surface.FULL, 3, 0, 3), "gap 2 diagonal");
    }

    @Test
    void diagonalJumpWithGapThreeIsTooFar() {
        assertFalse(valid(Surface.FULL, 4, 0, 4), "gap 3 diagonal covers about 4.2 blocks");
    }

    @Test
    void diagonalJumpWithGapFourIsTooFar() {
        assertFalse(valid(Surface.FULL, 5, 0, 5), "gap 4 diagonal");
    }

    @Test
    void touchingBlocksAreNoJump() {
        assertFalse(valid(Surface.FULL, 1, 0, 0), "gap 0");
    }

    @Test
    void ascentWithGapThreeIsReachable() {
        assertTrue(valid(Surface.FULL, 4, 1, 0), "gap 3 up");
    }

    @Test
    void ascentWithGapFourIsTooFar() {
        assertFalse(valid(Surface.FULL, 5, 1, 0), "gap 4 up");
    }

    @Test
    void descentWithGapFourIsReachable() {
        assertTrue(valid(Surface.FULL, 5, -1, 0), "gap 4 down");
    }

    @Test
    void fenceOnTheSameBlockHeightAsAFullBlockIsReachable() {
        assertTrue(valid(Surface.FENCE, 2, 0, 0), "fence top is only half a block above");
    }

    @Test
    void fenceOneBlockHigherThanAFullBlockIsUnreachable() {
        assertFalse(valid(Surface.FENCE, 2, 1, 0), "fence top would be 1.5 above the full block top");
    }

    @Test
    void fenceOneBlockLowerThanAFullBlockIsReachable() {
        assertTrue(valid(Surface.FENCE, 2, -1, 0), "fence");
    }

    @Test
    void fullBlockOneHigherThanASlabIsUnreachable() {
        assertFalse(validIn(new FakeSpaceProbe(), block(0, 10, 0, Surface.SLAB), block(2, 11, 0, Surface.FULL)), "slab top 10.5 to full top 12.0");
    }

    @Test
    void fullBlockOneHigherThanAFenceIsReachable() {
        assertTrue(validIn(new FakeSpaceProbe(), block(0, 10, 0, Surface.FENCE), block(2, 11, 0, Surface.FULL)), "fence top 11.5 to full top 12.0");
    }

    @Test
    void fullBlockOneHigherThanATrapdoorIsUnreachable() {
        assertFalse(validIn(new FakeSpaceProbe(), block(0, 10, 0, Surface.TRAPDOOR), block(2, 11, 0, Surface.FULL)), "trapdoor top 10.1875 to full top 12.0");
    }

    @Test
    void fullBlockAtTheLevelOfATrapdoorIsReachable() {
        assertTrue(validIn(new FakeSpaceProbe(), block(0, 10, 0, Surface.TRAPDOOR), block(2, 10, 0, Surface.FULL)), "trapdoor top 10.1875 to full top 11.0");
    }

    // --- room at the target ---------------------------------------------------------------------

    @Test
    void occupiedTargetIsRejected() {
        assertFalse(validIn(new FakeSpaceProbe().occupy(2, 10, 0), ORIGIN, block(2, 10, 0, Surface.FULL)), "target in wall");
    }

    @Test
    void blockOneAboveTheTargetBlocksTheHeadroom() {
        assertFalse(validIn(new FakeSpaceProbe().occupy(2, 11, 0), ORIGIN, block(2, 10, 0, Surface.FULL)), "y+1");
    }

    @Test
    void blockTwoAboveTheTargetBlocksTheHeadroom() {
        assertFalse(validIn(new FakeSpaceProbe().occupy(2, 12, 0), ORIGIN, block(2, 10, 0, Surface.FULL)), "y+2");
    }

    @Test
    void ceilingThreeBlocksAboveAFullTopLeavesNoRoomToJump() {
        assertFalse(validIn(new FakeSpaceProbe().occupy(2, 14, 0), ORIGIN, block(2, 10, 0, Surface.FULL)), "a player can stand under y=14 but not jump");
    }

    @Test
    void fourBlocksFreeAboveAFullTopLeaveRoomToJump() {
        assertTrue(validIn(new FakeSpaceProbe().occupy(2, 15, 0), ORIGIN, block(2, 10, 0, Surface.FULL)), "ceiling at y=15 is above the apex");
    }

    @Test
    void jumpRoomOfASlabNeedsOnlyItsOwnTop() {
        CourseBlock slab = block(2, 10, 0, Surface.SLAB);
        assertFalse(validIn(new FakeSpaceProbe().occupy(2, 13, 0), ORIGIN, slab), "slab top 10.5, apex head reaches y=13");
        assertTrue(validIn(new FakeSpaceProbe().occupy(2, 14, 0), ORIGIN, slab), "y=14 is above a slab's jump room");
    }

    @Test
    void blockThreeAboveAFenceTargetBlocksTheHeadroom() {
        assertFalse(validIn(new FakeSpaceProbe().occupy(2, 13, 0), ORIGIN, block(2, 10, 0, Surface.FENCE)), "fence y+3");
    }

    @Test
    void fenceTargetNeedsItsOwnJumpRoom() {
        CourseBlock fence = block(2, 10, 0, Surface.FENCE);
        assertFalse(validIn(new FakeSpaceProbe().occupy(2, 14, 0), ORIGIN, fence), "fence top 11.5, apex head reaches y=14");
        assertTrue(validIn(new FakeSpaceProbe().occupy(2, 15, 0), ORIGIN, fence), "y=15 is above the fence's jump room");
    }

    // --- flight path ----------------------------------------------------------------------------

    @Test
    void wallInTheGapBlocksTheFlight() {
        assertFalse(validIn(new FakeSpaceProbe().occupy(1, 11, 0), ORIGIN, block(2, 10, 0, Surface.FULL)), "wall at y+1");
    }

    @Test
    void wallTwoAboveTheTopInTheGapBlocksTheFlight() {
        assertFalse(validIn(new FakeSpaceProbe().occupy(1, 12, 0), ORIGIN, block(2, 10, 0, Surface.FULL)), "ceiling at y+2");
    }

    @Test
    void overhangCuttingTheApexOverTheGapBlocksTheFlight() {
        assertFalse(validIn(new FakeSpaceProbe().occupy(1, 14, 0), ORIGIN, block(3, 10, 0, Surface.FULL)), "ceiling at y=14 over the gap cuts the apex");
    }

    @Test
    void overhangAboveTheApexOverTheGapLeavesTheFlightFree() {
        assertTrue(validIn(new FakeSpaceProbe().occupy(1, 15, 0), ORIGIN, block(3, 10, 0, Surface.FULL)), "ceiling at y=15 is above the apex");
    }

    @Test
    void floorUnderTheGapDoesNotBlockTheFlight() {
        assertTrue(validIn(new FakeSpaceProbe().occupyBox(-5, 0, -5, 10, 9, 5), ORIGIN, block(3, 10, 0, Surface.FULL)), "lobby floor below the gap is fine");
    }

    @Test
    void obstacleBesideTheStraightLineDoesNotBlockTheFlight() {
        assertTrue(validIn(new FakeSpaceProbe().occupy(1, 11, 1), ORIGIN, block(2, 10, 0, Surface.FULL)), "beside the line");
    }

    @Test
    void flightBetweenTwoSlabsNeedsTheirOwnLevelFree() {
        CourseBlock slab = block(0, 10, 0, Surface.SLAB);
        assertFalse(validIn(new FakeSpaceProbe().occupy(1, 10, 0), slab, block(2, 10, 0, Surface.SLAB)), "player walks off a slab at its own level");
    }

    @Test
    void diagonalStepNeedsBothNeighbourCellsFreeFirst() {
        assertFalse(validIn(new FakeSpaceProbe().occupy(1, 11, 0), ORIGIN, block(2, 10, 2, Surface.FULL)), "x neighbour");
    }

    @Test
    void diagonalStepNeedsBothNeighbourCellsFreeSecond() {
        assertFalse(validIn(new FakeSpaceProbe().occupy(0, 11, 1), ORIGIN, block(2, 10, 2, Surface.FULL)), "z neighbour");
    }

    @Test
    void diagonalStepNeedsTheDiagonalCellFree() {
        assertFalse(validIn(new FakeSpaceProbe().occupy(1, 11, 1), ORIGIN, block(2, 10, 2, Surface.FULL)), "centre");
    }

    @Test
    void openDiagonalIsValid() {
        assertTrue(valid(Surface.FULL, 2, 0, 2), "open diagonal");
    }

    // --- world bounds ---------------------------------------------------------------------------

    @Test
    void targetOutsideTheBoundsIsRejected() {
        FakeSpaceProbe world = new FakeSpaceProbe(new BlockPos(-50, 0, -50), new BlockPos(1, 100, 50));
        assertFalse(validIn(world, ORIGIN, block(2, 10, 0, Surface.FULL)), "x beyond the border");
    }

    @Test
    void targetBelowTheBoundsIsRejected() {
        assertFalse(validIn(new FakeSpaceProbe(new BlockPos(-50, 10, -50), new BlockPos(50, 100, 50)), ORIGIN, block(2, 9, 0, Surface.FULL)), "below the world");
    }

    // --- lobby height band ------------------------------------------------------------------------

    @Test
    void targetAtTheLobbyUpperLimitIsValidAndOneAboveIsNot() {
        FakeSpaceProbe world = new FakeSpaceProbe();
        HeightBand band = new HeightBand(TestBlocks.bounds(0, 100));
        assertTrue(validIn(world, band, block(0, 95, 0, Surface.FULL), block(2, 96, 0, Surface.FULL)), "top 97 + 2.2522 fits under 100");
        assertFalse(validIn(world, band, block(0, 96, 0, Surface.FULL), block(2, 97, 0, Surface.FULL)), "top 98 + 2.2522 passes 100");
    }

    @Test
    void targetAboveTheBandIsInvalidEvenWhenTheDimensionHasRoom() {
        FakeSpaceProbe world = new FakeSpaceProbe();
        HeightBand band = new HeightBand(TestBlocks.bounds(0, 60));
        assertFalse(validIn(world, band, block(0, 57, 0, Surface.FULL), block(2, 58, 0, Surface.FULL)), "otherwise valid, but the lobby ends at 60");
    }

    @Test
    void targetBelowTheBandIsInvalidWhileTheDimensionStillHasRoom() {
        FakeSpaceProbe world = new FakeSpaceProbe(new BlockPos(-50, -64, -50), new BlockPos(50, 100, 50));
        HeightBand band = new HeightBand(TestBlocks.bounds(0, 310));
        assertTrue(validIn(world, band, block(0, 8, 0, Surface.FULL), block(2, 7, 0, Surface.FULL)), "top 8 keeps the fall allowance above 0");
        assertFalse(validIn(world, band, block(0, 7, 0, Surface.FULL), block(2, 6, 0, Surface.FULL)), "top 7 lets the fall reach 0");
    }
}
