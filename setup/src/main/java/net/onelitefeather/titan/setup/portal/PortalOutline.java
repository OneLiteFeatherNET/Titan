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
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The particle points that trace a saved portal shape for {@code show}. Pure geometry, so it is
 * testable without a server.
 */
public final class PortalOutline {

    /**
     * Upper bound for the points of a shape; more particles per tick would only lag the client.
     * {@link #points(Portal)} adds the label anchor's {@link #ANCHOR_POINTS} on top.
     */
    public static final int MAX_POINTS = 256;

    /** The label anchor's cross: the point itself and one on each side of every axis. */
    public static final int ANCHOR_POINTS = 7;

    /** How far the cross arms reach from the anchor, in blocks. */
    private static final double ANCHOR_ARM = 0.3;

    /** Wanted distance between neighbouring points, in blocks. */
    private static final double STEP = 0.5;

    /** A ring never gets fewer points than this, however small it is. */
    private static final int MIN_RING_POINTS = 16;

    private PortalOutline() {
    }

    /** The points along the shape's edges (box) or rim (disc); at most {@link #MAX_POINTS}. */
    public static @NotNull List<Vec> points(@NotNull PortalShape shape) {
        return switch (shape) {
            case Box box -> boxEdges(box.min(), box.max().add(1, 1, 1), MAX_POINTS);
            case Disc disc -> circle(disc.center(), disc.radius(), disc.normal(), MAX_POINTS);
        };
    }

    /**
     * The shape's points plus the label anchor's cross when the portal has an anchor: at most
     * {@link #MAX_POINTS} + {@link #ANCHOR_POINTS}.
     */
    public static @NotNull List<Vec> points(@NotNull Portal portal) {
        List<Vec> points = new ArrayList<>(points(portal.shape()));
        points.addAll(anchor(portal.label() == null ? null : portal.label().position()));
        return List.copyOf(points);
    }

    /** A small cross marking where a label floats; nothing while there is no anchor. */
    public static @NotNull List<Vec> anchor(@Nullable Vec position) {
        if (position == null) {
            return List.of();
        }
        return List.of(position, position.add(ANCHOR_ARM, 0, 0), position.sub(ANCHOR_ARM, 0, 0), position.add(0, ANCHOR_ARM, 0), position.sub(0, ANCHOR_ARM, 0), position.add(0, 0, ANCHOR_ARM), position.sub(0, 0, ANCHOR_ARM));
    }

    /**
     * Points on the twelve edges of the box {@code [lo, hi]} in world coordinates, the eight
     * corners included. When the budget is too small the spacing grows instead of points being
     * dropped, so the outline stays a complete box.
     */
    static @NotNull List<Vec> boxEdges(@NotNull Vec lo, @NotNull Vec hi, int budget) {
        double[] origin = {lo.x(), lo.y(), lo.z()};
        double[] length = {hi.x() - lo.x(), hi.y() - lo.y(), hi.z() - lo.z()};
        double step = STEP;
        while (total(length, step) > budget) {
            step *= 1.05;
        }
        List<Vec> points = new ArrayList<>(total(length, step));
        for (int corner = 0; corner < 8; corner++) {
            points.add(new Vec(origin[0] + (corner & 1) * length[0], origin[1] + (corner >> 1 & 1) * length[1], origin[2] + (corner >> 2 & 1) * length[2]));
        }
        // The corners are in already; each edge adds only the points strictly between them.
        for (int axis = 0; axis < 3; axis++) {
            int segments = segments(length[axis], step);
            for (int side = 0; side < 4; side++) {
                for (int i = 1; i < segments; i++) {
                    double[] at = origin.clone();
                    at[axis] += length[axis] * i / segments;
                    at[(axis + 1) % 3] += (side & 1) * length[(axis + 1) % 3];
                    at[(axis + 2) % 3] += (side >> 1) * length[(axis + 2) % 3];
                    points.add(new Vec(at[0], at[1], at[2]));
                }
            }
        }
        return List.copyOf(points);
    }

    /**
     * Points on the circle around {@code centre} in the plane perpendicular to {@code normal}. A
     * non-positive radius or a zero normal has no circle and yields no points.
     */
    static @NotNull List<Vec> circle(@NotNull Vec centre, double radius, @NotNull Vec normal, int budget) {
        if (!(radius > 0) || !(normal.length() > 0)) {
            return List.of();
        }
        Vec n = normal.normalize();
        // Cross with an axis the normal is not close to, else the cross product degenerates.
        Vec helper = Math.abs(n.y()) < 0.9 ? new Vec(0, 1, 0) : new Vec(1, 0, 0);
        Vec u = n.cross(helper).normalize();
        Vec v = n.cross(u);
        int count = Math.min(budget, Math.max(MIN_RING_POINTS, (int) Math.ceil(2 * Math.PI * radius / STEP)));
        List<Vec> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            points.add(centre.add(u.mul(radius * Math.cos(angle))).add(v.mul(radius * Math.sin(angle))));
        }
        return List.copyOf(points);
    }

    private static int segments(double length, double step) {
        return Math.max(1, (int) Math.ceil(length / step));
    }

    private static int total(double[] length, double step) {
        int between = 0;
        for (double l : length) {
            between += segments(l, step) - 1;
        }
        return 8 + 4 * between;
    }
}
