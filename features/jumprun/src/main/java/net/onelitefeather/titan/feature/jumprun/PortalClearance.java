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

import java.util.List;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Disc;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalShape;

/**
 * Keeps a jump away from the lobby portals, so a run never sends its player to another server by
 * flying through one. Pure geometry on the portal shapes, like {@link Clearance} on the blocks.
 */
final class PortalClearance {

    /** Blocks of air the target and the flight path keep to a portal. */
    static final int MARGIN = 3;

    /** Half the diagonal of a unit cube: how far a cell reaches from its centre. */
    private static final double CELL_REACH = Math.sqrt(3.0) / 2.0;

    static final PortalClearance NONE = new PortalClearance(List.of());

    private final List<PortalShape> shapes;

    private PortalClearance(List<PortalShape> shapes) {
        this.shapes = shapes;
    }

    static PortalClearance of(List<PortalShape> shapes) {
        return shapes.isEmpty() ? NONE : new PortalClearance(List.copyOf(shapes));
    }

    static PortalClearance ofPortals(List<Portal> portals) {
        return of(portals.stream().map(Portal::shape).toList());
    }

    /**
     * Whether the target and every cell of the flight path, over the height the jump and a
     * standing player reach, stay {@link #MARGIN} blocks from each portal.
     */
    boolean isKept(Jump jump) {
        if (shapes.isEmpty()) {
            return true;
        }
        int low = Math.min(jump.from().pos().y(), jump.to().pos().y());
        int high = Math.max(jump.from().headroomTopY(), jump.to().headroomTopY());
        return FlightPath.cellsUpTo(jump.from().pos(), jump.to().pos()).stream().noneMatch(cell -> isNearAnywhere(cell, low, high));
    }

    private boolean isNearAnywhere(FlightPath.Cell cell, int low, int high) {
        for (int y = low; y <= high; y++) {
            for (PortalShape shape : shapes) {
                if (isNear(shape, cell.x(), y, cell.z())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isNear(PortalShape shape, int x, int y, int z) {
        return switch (shape) {
            case Box box -> isNear(box, x, y, z);
            case Disc disc -> isNear(disc, x, y, z);
        };
    }

    /** The box grown by the margin on every side contains the cell, touching not counting. */
    private static boolean isNear(Box box, int x, int y, int z) {
        return gap(x, box.min().x(), box.max().x() + 1) < MARGIN && gap(y, box.min().y(), box.max().y() + 1) < MARGIN && gap(z, box.min().z(), box.max().z() + 1) < MARGIN;
    }

    /** Distance along one axis between the cell {@code [at, at + 1]} and {@code [lo, hi]}. */
    private static double gap(int at, double lo, double hi) {
        return Math.max(0.0, Math.max(lo - (at + 1), at - hi));
    }

    /**
     * The distance from the cell's centre to the disc, less what the cell reaches around its
     * centre: a little too careful, which only costs a candidate, never a portal.
     */
    private static boolean isNear(Disc disc, int x, int y, int z) {
        Vec offset = new Vec(x + 0.5 - disc.center().x(), y + 0.5 - disc.center().y(), z + 0.5 - disc.center().z());
        double height = offset.dot(disc.normal());
        double inPlane = offset.sub(disc.normal().mul(height)).length();
        double distance = Math.hypot(height, Math.max(0.0, inPlane - disc.radius()));
        return distance - CELL_REACH < MARGIN;
    }
}
