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
package net.onelitefeather.titan.setup.portal;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Disc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscPlacementTest {

    private static final Pos ORIGIN = new Pos(0, 0, 0);

    /** A direction that deviates from {@code axis} by {@code degrees} towards {@code towards}. */
    private static Vec tilted(Vec axis, Vec towards, double degrees) {
        double radians = Math.toRadians(degrees);
        return axis.mul(Math.cos(radians)).add(towards.mul(Math.sin(radians)));
    }

    @Test
    @DisplayName("Rounds every centre coordinate to the nearest half block")
    void roundsCentreToHalfBlocks() {
        Disc disc = DiscPlacement.of(new Pos(0.52, 72.0, 40.47), new Vec(0, 0, 1), 5.5);

        assertEquals(new Vec(0.5, 72.0, 40.5), disc.center(), "centre is rounded to 0.5");
        assertEquals(5.5, disc.radius(), "radius is passed through");
    }

    @Test
    @DisplayName("Rounds negative coordinates to the nearest half block")
    void roundsNegativeCentre() {
        Disc disc = DiscPlacement.of(new Pos(-0.26, -10.74, -3.0), new Vec(0, 0, 1), 1);

        assertEquals(new Vec(-0.5, -10.5, -3.0), disc.center(), "negative values round to the nearest 0.5");
    }

    @Test
    @DisplayName("Snaps a look direction 2 degrees off +Z onto +Z")
    void snapsNearPositiveZ() {
        Disc disc = DiscPlacement.of(ORIGIN, tilted(new Vec(0, 0, 1), new Vec(1, 0, 0), 2), 3);

        assertEquals(new Vec(0, 0, 1), disc.normal(), "normal snaps to +Z");
    }

    @Test
    @DisplayName("Snaps to each of the six axes when 2 degrees off")
    void snapsToEverySixAxis() {
        Vec[] axes = {new Vec(1, 0, 0), new Vec(-1, 0, 0), new Vec(0, 1, 0), new Vec(0, -1, 0), new Vec(0, 0, 1), new Vec(0, 0, -1)};
        for (Vec axis : axes) {
            Vec towards = axis.y() == 0 ? new Vec(0, 1, 0) : new Vec(1, 0, 0);

            Disc disc = DiscPlacement.of(ORIGIN, tilted(axis, towards, 2), 3);

            assertEquals(axis, disc.normal(), "2 degrees off " + axis + " snaps to it");
        }
    }

    @Test
    @DisplayName("Snaps a direction exactly on the axis")
    void snapsExactAxis() {
        assertEquals(new Vec(0, 1, 0), DiscPlacement.of(ORIGIN, new Vec(0, 1, 0), 3).normal(), "looking straight up gives +Y");
        assertEquals(new Vec(0, -1, 0), DiscPlacement.of(ORIGIN, new Vec(0, -1, 0), 3).normal(), "looking straight down gives -Y");
    }

    @Test
    @DisplayName("Keeps the exact direction when it is 6 degrees off an axis")
    void doesNotSnapAtSixDegrees() {
        Vec look = tilted(new Vec(0, 0, 1), new Vec(1, 0, 0), 6);

        Disc disc = DiscPlacement.of(ORIGIN, look, 3);

        assertEquals(look.x(), disc.normal().x(), 1e-9, "x stays");
        assertEquals(look.z(), disc.normal().z(), 1e-9, "z stays");
        assertEquals(1.0, disc.normal().length(), 1e-9, "normal is a unit vector");
    }

    @Test
    @DisplayName("Snaps just inside 5 degrees and not just outside")
    void snapsAtTheFiveDegreeBoundary() {
        assertEquals(new Vec(0, 0, 1), DiscPlacement.of(ORIGIN, tilted(new Vec(0, 0, 1), new Vec(1, 0, 0), 4.99), 3).normal(), "4.99 degrees snaps");
        assertTrue(DiscPlacement.of(ORIGIN, tilted(new Vec(0, 0, 1), new Vec(1, 0, 0), 5.01), 3).normal().x() > 0, "5.01 degrees does not snap");
    }

    @Test
    @DisplayName("Normalises a diagonal look direction without snapping")
    void normalisesDiagonal() {
        Disc disc = DiscPlacement.of(ORIGIN, new Vec(3, 0, 3), 3);

        double component = Math.sqrt(0.5);
        assertEquals(component, disc.normal().x(), 1e-9, "x of the diagonal");
        assertEquals(0.0, disc.normal().y(), 1e-9, "y of the diagonal");
        assertEquals(component, disc.normal().z(), 1e-9, "z of the diagonal");
    }

    @Test
    @DisplayName("Snaps an unnormalised look vector near an axis")
    void snapsUnnormalisedLook() {
        assertEquals(new Vec(1, 0, 0), DiscPlacement.of(ORIGIN, new Vec(10, 0.1, 0), 3).normal(), "length does not matter for snapping");
    }

    @Test
    @DisplayName("Rejects a look vector of length zero")
    void rejectsZeroLook() {
        assertThrows(IllegalArgumentException.class, () -> DiscPlacement.of(ORIGIN, Vec.ZERO, 3), "a zero direction has no plane");
    }
}
