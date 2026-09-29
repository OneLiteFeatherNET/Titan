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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DraftOutlineTest {

    private static final double EPSILON = 1e-9;

    @Test
    void constantsArePinned() {
        assertEquals(256, DraftOutline.MAX_POINTS, "the preview cap is a documented contract");
        assertEquals(3.0, DraftOutline.DEFAULT_RADIUS, "default ring radius");
        assertEquals(List.of(2.0, 3.0, 5.0, 8.0), DraftOutline.RADIUS_SUGGESTIONS, "radius suggestions");
    }

    @Test
    void boxWithoutCornersHasNoPoints() {
        assertTrue(DraftOutline.box(List.of(), new Vec(1, 2, 3)).isEmpty(), "nothing set, nothing to show");
    }

    @Test
    void cornerIsMarkedAtItsBlockCentre() {
        List<Vec> points = DraftOutline.box(List.of(new Vec(4, 65, -2)), new Vec(4, 65, -2));

        assertTrue(points.contains(new Vec(4.5, 65.5, -1.5)), "block centre marker");
    }

    @Test
    void singleCornerBoxRunsFromCornerToCurrentBlock() {
        List<Vec> points = DraftOutline.box(List.of(new Vec(0, 0, 0)), new Vec(3, 1, 2));

        assertTrue(points.contains(new Vec(4, 2, 3)), "far corner is block + 1");
        assertTrue(points.contains(Vec.ZERO), "near corner is the set corner");
        assertTrue(points.contains(new Vec(0.5, 0.5, 0.5)), "marker of the set corner");
    }

    @Test
    void twoCornerBoxIgnoresOrderAndCurrentBlock() {
        List<Vec> a = DraftOutline.box(List.of(new Vec(0, 0, 0), new Vec(3, 1, 2)), new Vec(9, 9, 9));
        List<Vec> b = DraftOutline.box(List.of(new Vec(3, 1, 2), new Vec(0, 0, 0)), new Vec(9, 9, 9));

        assertEquals(Set.copyOf(a), Set.copyOf(b), "same two corners, same box");
        assertTrue(a.contains(new Vec(4, 2, 3)), "box spans both corners");
        assertFalse(a.contains(new Vec(10, 10, 10)), "the current block is ignored once both corners are set");
    }

    @Test
    void hugeBoxRespectsCapAcrossMarkersAndKeepsCorners() {
        List<Vec> points = DraftOutline.box(List.of(new Vec(0, 0, 0), new Vec(999, 999, 999)), Vec.ZERO);

        assertTrue(points.size() <= DraftOutline.MAX_POINTS, "cap exceeded: " + points.size());
        assertTrue(points.contains(new Vec(1000, 1000, 1000)), "far corner kept");
        assertTrue(points.contains(new Vec(0.5, 0.5, 0.5)), "marker kept");
    }

    @Test
    void ringWithDiscUsesItsCentreRadiusAndNormal() {
        List<Vec> points = DraftOutline.ring(new Disc(new Vec(1, 2, 3), 5, new Vec(0, 0, 1)), new Pos(100, 100, 100), new Vec(1, 0, 0));

        assertRing(points, new Vec(1, 2, 3), 5, new Vec(0, 0, 1));
    }

    @Test
    void ringWithoutRadiusUsesDefaultRadiusAroundEyeFacingLook() {
        for (Vec look : List.of(new Vec(0, 0, 1), new Vec(0, 1, 0), new Vec(1, 1, 1), new Vec(0.2, -0.9, 0.1))) {
            List<Vec> points = DraftOutline.ring(null, new Pos(5, 70, 5), look);

            assertRing(points, new Vec(5, 70, 5), 3, look);
        }
    }

    @Test
    void hugeRingRespectsCap() {
        List<Vec> points = DraftOutline.ring(new Disc(Vec.ZERO, 5000, new Vec(0, 1, 0)), Pos.ZERO, new Vec(0, 0, 1));

        assertEquals(DraftOutline.MAX_POINTS, points.size(), "cap applies to ring previews");
    }

    private static void assertRing(List<Vec> points, Vec centre, double radius, Vec normal) {
        Vec unit = normal.normalize();
        assertTrue(points.size() >= 16, "too few ring points");
        for (Vec p : points) {
            assertEquals(radius, p.distance(centre), EPSILON, "distance from centre");
            assertEquals(0.0, p.sub(centre).dot(unit), EPSILON, "not perpendicular to the normal");
        }
    }
}
