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
package net.onelitefeather.titan.core.portal;

import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxTest {

    /** Blocks 10..14 x 64..68 x 10..11 inclusive, so world [10,15] x [64,69] x [10,12]. */
    private static final Box BOX = new Box(new Vec(10, 64, 10), new Vec(14, 68, 11));

    private static void assertCrossed(Vec from, Vec to, String why) {
        assertTrue(BOX.crossedBy(from, to), why);
    }

    private static void assertNotCrossed(Vec from, Vec to, String why) {
        assertFalse(BOX.crossedBy(from, to), why);
    }

    @Test
    void countsASegmentThroughTheBox() {
        assertCrossed(new Vec(12, 66, 5), new Vec(12, 66, 20), "entering and leaving the box is a crossing");
    }

    @Test
    void countsASegmentEndingInsideTheBox() {
        assertCrossed(new Vec(12, 66, 5), new Vec(12, 66, 11), "ending inside is a crossing");
    }

    @Test
    void countsASegmentStartingInsideTheBox() {
        assertCrossed(new Vec(12, 66, 11), new Vec(12, 66, 20), "starting inside is a crossing");
    }

    @Test
    void countsASegmentInsideTheBoxOnBothEnds() {
        assertCrossed(new Vec(11, 65, 10.5), new Vec(13, 67, 11.5), "staying inside is a crossing");
    }

    @Test
    void rejectsASegmentPassingBesideTheBox() {
        assertNotCrossed(new Vec(8, 66, 5), new Vec(8, 66, 20), "a segment two blocks beside the box misses it");
    }

    @Test
    void rejectsADiagonalSegmentThatPassesTheCornerOutside() {
        // Inside the x slab for t in [0.5, 0.75] but inside the z slab for t in [0.35, 0.45]: never both.
        assertNotCrossed(new Vec(0, 66, 19), new Vec(20, 66, -1), "slab intervals do not overlap, so it misses");
    }

    @Test
    void countsAnAxisParallelSegmentInsideTheSlab() {
        assertCrossed(new Vec(5, 66, 11), new Vec(20, 66, 11), "moving along x inside the y and z slabs hits");
    }

    @Test
    void rejectsAnAxisParallelSegmentOutsideTheSlab() {
        assertNotCrossed(new Vec(5, 70, 11), new Vec(20, 70, 11), "moving along x above the y slab misses");
    }

    @Test
    void countsAGrazingSegmentAlongAnEdge() {
        assertCrossed(new Vec(5, 64, 10), new Vec(20, 64, 10), "touching the lower edge counts");
    }

    @Test
    void countsAGrazingSegmentThroughACorner() {
        assertCrossed(new Vec(9, 63, 9), new Vec(11, 65, 11), "passing through the lowest corner counts");
    }

    @Test
    void countsAPositionOnTheInclusiveMaxPlusOneFace() {
        assertCrossed(new Vec(15, 66, 11), new Vec(16, 66, 11), "max + 1 is inclusive");
    }

    @Test
    void rejectsAPositionJustBeyondMaxPlusOne() {
        assertNotCrossed(new Vec(15.0001, 66, 11), new Vec(16, 66, 11), "max + 1.0001 is outside");
    }

    @Test
    void countsAZeroLengthStepInsideTheBox() {
        Vec standingStill = new Vec(12, 66, 11);

        assertCrossed(standingStill, standingStill, "standing inside the box is containment");
    }

    @Test
    void rejectsAZeroLengthStepOutsideTheBox() {
        Vec standingStill = new Vec(20, 66, 11);

        assertNotCrossed(standingStill, standingStill, "standing outside the box is no crossing");
    }
}
