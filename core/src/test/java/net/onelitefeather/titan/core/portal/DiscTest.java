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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscTest {

    /** Facing +z, centred on the origin's column. */
    private static final Disc FLAT = new Disc(new Vec(0, 64, 100), 5.0, new Vec(0, 0, 1));

    /** Tilted in the xy plane so that all three dot-product terms stay alive. */
    private static final Disc TILTED = new Disc(new Vec(10, 70, -20), 4.0, new Vec(0.6, 0.8, 0));

    private static void assertCrossed(Disc disc, Vec from, Vec to, String why) {
        assertTrue(disc.crossedBy(from, to), why);
    }

    private static void assertNotCrossed(Disc disc, Vec from, Vec to, String why) {
        assertFalse(disc.crossedBy(from, to), why);
    }

    @Test
    void countsAPassStraightThroughTheCentre() {
        assertCrossed(FLAT, new Vec(0, 64, 99), new Vec(0, 64, 101), "a pass through the centre must count");
    }

    @Test
    void countsAPassExactlyOnTheRim() {
        assertCrossed(FLAT, new Vec(5, 64, 99), new Vec(5, 64, 101), "the rim is inclusive");
    }

    @Test
    void rejectsAPassJustOutsideTheRim() {
        assertNotCrossed(FLAT, new Vec(5.001, 64, 99), new Vec(5.001, 64, 101), "outside the rim must not count");
    }

    @Test
    void rejectsMovementParallelToThePlane() {
        assertNotCrossed(FLAT, new Vec(0, 64, 99), new Vec(10, 64, 99), "a step in the plane never crosses it");
    }

    @Test
    void rejectsAPlaneTheSegmentStopsShortOf() {
        assertNotCrossed(FLAT, new Vec(0, 64, 90), new Vec(0, 64, 95), "the plane at z=100 is not reached");
    }

    @Test
    void rejectsAPlaneAlreadyBehindTheSegment() {
        assertNotCrossed(FLAT, new Vec(0, 64, 101), new Vec(0, 64, 105), "moving away from the plane must not count");
    }

    @Test
    void countsABackwardsPass() {
        assertCrossed(FLAT, new Vec(0, 64, 101), new Vec(0, 64, 99), "direction is not checked");
    }

    @Test
    void countsAPassThroughATiltedDiscsCentre() {
        assertCrossed(TILTED, new Vec(9.4, 69.2, -20), new Vec(10.6, 70.8, -20), "tilted centre pass must count");
    }

    @Test
    void countsAnOffCentreHitOnATiltedDisc() {
        assertCrossed(TILTED, new Vec(9.4, 69.2, -17), new Vec(10.6, 70.8, -17), "3 < radius 4");
    }

    @Test
    void rejectsAnOffCentreMissOnATiltedDisc() {
        assertNotCrossed(TILTED, new Vec(9.4, 69.2, -15), new Vec(10.6, 70.8, -15), "5 > radius 4");
    }

    @Test
    void rejectsAZeroLengthStep() {
        Vec standingStill = new Vec(0, 64, 100);

        assertNotCrossed(FLAT, standingStill, standingStill, "a stationary player must not trigger (no NaN)");
    }

    @Test
    void countsARimPassWithACombinedXAndYOffset() {
        // 3-4-5 triangle: an offset of (3, -4) is exactly radius 5 away.
        assertCrossed(FLAT, new Vec(3, 60, 99), new Vec(3, 60, 101), "distance 5 on the rim counts");
    }

    @Test
    void rejectsAPassJustOutsideTheRimWithACombinedXAndYOffset() {
        assertNotCrossed(FLAT, new Vec(3.001, 60, 99), new Vec(3.001, 60, 101), "both offsets must feed the distance");
    }

    @Test
    void countsARimPassOnATiltedDiscThroughAGenuineZStep() {
        assertCrossed(TILTED, new Vec(9.4, 69.2, -18), new Vec(10.6, 70.8, -14), "interpolated z lands on the rim");
    }

    @Test
    void rejectsAPassJustOutsideTheRimOnATiltedDiscThroughAGenuineZStep() {
        assertNotCrossed(TILTED, new Vec(9.4, 69.2, -18), new Vec(10.6, 70.8, -13.996), "interpolated z is past the rim");
    }

    @Test
    void detectsAnEightBlockElytraStepThatSkipsAOneBlockThinDisc() {
        Disc thin = new Disc(new Vec(0.5, 72, 40.5), 5.5, new Vec(0, 0, 1));

        assertCrossed(thin, new Vec(0.5, 72, 36.5), new Vec(0.5, 72, 44.5), "a fast step over the plane must be detected although neither end is near it");
    }

    @Test
    void detectsAStepThroughADiscWhoseNormalIsNotAUnitVector() {
        Disc handWritten = new Disc(new Vec(0, 64, 100), 5.0, new Vec(0, 0, 4));

        assertCrossed(handWritten, new Vec(0, 64, 99), new Vec(0, 64, 101), "normal is normalised before use");
    }

    @Test
    void exposesTheNormalAsAUnitVector() {
        Disc handWritten = new Disc(new Vec(0, 64, 100), 5.0, new Vec(0, 3, 4));

        assertEquals(new Vec(0, 0.6, 0.8), handWritten.normal(), "the normal must be normalised");
    }

    @Test
    void keepsAZeroLengthNormalForTheValidatorToReject() {
        Disc broken = new Disc(new Vec(0, 64, 100), 5.0, Vec.ZERO);

        assertEquals(Vec.ZERO, broken.normal(), "a zero normal must not turn into NaN");
    }

    @Test
    void boundsTheDiscByCenterPlusMinusRadius() {
        HorizontalBounds bounds = new Disc(new Vec(10, 64, -20), 4.0, new Vec(0, 0, 1)).horizontalBounds();

        assertEquals(new HorizontalBounds(6, -24, 14, -16), bounds, "conservative rectangle around the centre");
    }
}
