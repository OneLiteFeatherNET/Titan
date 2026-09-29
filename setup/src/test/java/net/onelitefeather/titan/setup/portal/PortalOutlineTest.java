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

import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Disc;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalOutlineTest {

    private static final double EPSILON = 1e-9;

    @Test
    void capIsPinned() {
        assertEquals(256, PortalOutline.MAX_POINTS, "the per-portal cap is a documented contract");
    }

    @Test
    void unitBlockBoxHasCornersAndEdgeMidpoints() {
        List<Vec> points = PortalOutline.points(new Box(new Vec(0, 0, 0), new Vec(0, 0, 0)));

        // 8 corners + 12 edges with one midpoint each.
        assertEquals(20, points.size(), "unit block outline");
        assertEquals(20, new HashSet<>(points).size(), "no point is emitted twice");
    }

    @Test
    void boxPointsLieOnlyOnEdgesOfMinToMaxPlusOne() {
        List<Vec> points = PortalOutline.points(new Box(new Vec(2, 3, 4), new Vec(5, 4, 8)));

        for (Vec p : points) {
            assertTrue(onEdge(p, new Vec(2, 3, 4), new Vec(6, 5, 9)), "point " + p + " is not on a box edge");
        }
    }

    @Test
    void boxContainsAllEightCorners() {
        List<Vec> points = PortalOutline.points(new Box(new Vec(2, 3, 4), new Vec(5, 4, 8)));

        for (double x : new double[]{2, 6}) {
            for (double y : new double[]{3, 5}) {
                for (double z : new double[]{4, 9}) {
                    assertTrue(points.contains(new Vec(x, y, z)), "missing corner " + x + "," + y + "," + z);
                }
            }
        }
    }

    @Test
    void boxEdgeSpacingIsAtMostHalfABlock() {
        List<Vec> points = PortalOutline.points(new Box(new Vec(0, 0, 0), new Vec(2, 0, 0)));

        List<Double> xs = points.stream().filter(p -> p.y() == 0 && p.z() == 0).map(Vec::x).sorted().toList();
        for (int i = 1; i < xs.size(); i++) {
            assertTrue(xs.get(i) - xs.get(i - 1) <= 0.5 + EPSILON, "gap too large at " + xs.get(i));
        }
        assertEquals(0.0, xs.getFirst(), EPSILON, "edge starts at min");
        assertEquals(3.0, xs.getLast(), EPSILON, "edge ends at max + 1");
    }

    @Test
    void hugeBoxRespectsCapAndKeepsCorners() {
        List<Vec> points = PortalOutline.points(new Box(new Vec(0, 0, 0), new Vec(999, 999, 999)));

        assertTrue(points.size() <= PortalOutline.MAX_POINTS, "cap exceeded: " + points.size());
        assertTrue(points.contains(new Vec(1000, 1000, 1000)), "the box stays complete, far corner kept");
        assertTrue(points.contains(Vec.ZERO), "the box stays complete, near corner kept");
    }

    @Test
    void ringHasAtLeastSixteenPointsEvenWhenTiny() {
        List<Vec> points = PortalOutline.points(new Disc(Vec.ZERO, 0.1, new Vec(0, 1, 0)));

        assertEquals(16, points.size(), "minimum ring resolution");
    }

    @Test
    void ringPointCountFollowsCircumference() {
        List<Vec> points = PortalOutline.points(new Disc(Vec.ZERO, 4, new Vec(0, 1, 0)));

        assertEquals((int) Math.ceil(2 * Math.PI * 4 / 0.5), points.size(), "one point per half block of rim");
    }

    @Test
    void hugeRingRespectsCap() {
        List<Vec> points = PortalOutline.points(new Disc(Vec.ZERO, 500, new Vec(1, 1, 1)));

        assertEquals(PortalOutline.MAX_POINTS, points.size(), "cap applies to rings");
    }

    @Test
    void ringPointsAreAtRadiusAndPerpendicularForAxisNormals() {
        for (Vec normal : List.of(new Vec(1, 0, 0), new Vec(0, 1, 0), new Vec(0, 0, 1), new Vec(0, -1, 0))) {
            assertRing(new Vec(10, 64, -3), 5, normal);
        }
    }

    @Test
    void ringPointsAreAtRadiusAndPerpendicularForTiltedAndDiagonalNormals() {
        assertRing(new Vec(0.5, 70, 0.5), 3, new Vec(1, 1, 1));
        assertRing(Vec.ZERO, 2.5, new Vec(0.01, 1, 0.02));
        assertRing(Vec.ZERO, 2.5, new Vec(0, 3, 0));
    }

    @Test
    void ringWithUnusableNormalOrRadiusHasNoPoints() {
        assertTrue(PortalOutline.points(new Disc(Vec.ZERO, 3, Vec.ZERO)).isEmpty(), "zero normal");
        assertTrue(PortalOutline.points(new Disc(Vec.ZERO, 0, new Vec(0, 1, 0))).isEmpty(), "zero radius");
    }

    private static void assertRing(Vec centre, double radius, Vec normal) {
        List<Vec> points = PortalOutline.points(new Disc(centre, radius, normal));
        Vec unit = normal.normalize();
        assertTrue(points.size() >= 16, "too few points for normal " + normal);
        for (Vec p : points) {
            assertEquals(radius, p.distance(centre), EPSILON, "distance from centre for normal " + normal);
            assertEquals(0.0, p.sub(centre).dot(unit), EPSILON, "not in the plane for normal " + normal);
        }
    }

    private static boolean onEdge(Vec p, Vec lo, Vec hi) {
        double[] v = {p.x(), p.y(), p.z()};
        double[] l = {lo.x(), lo.y(), lo.z()};
        double[] h = {hi.x(), hi.y(), hi.z()};
        int atBound = 0;
        for (int axis = 0; axis < 3; axis++) {
            if (v[axis] < l[axis] - EPSILON || v[axis] > h[axis] + EPSILON) {
                return false;
            }
            if (Math.abs(v[axis] - l[axis]) < EPSILON || Math.abs(v[axis] - h[axis]) < EPSILON) {
                atBound++;
            }
        }
        return atBound >= 2;
    }
}
