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

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;

/**
 * Where a player stands to face a {@link Portal} without triggering it.
 *
 * <p>The front is the side the portal's label floats on, since the map author puts the label
 * where players approach; without a label it is the side the {@code reference} point is on.
 */
public final class PortalApproach {

    /** A standing player's eye height above their feet. */
    public static final double EYE_HEIGHT = 1.62;

    /** Clearance between the player's feet and the box face or disc plane. */
    private static final double CLEARANCE = 2.0;

    /** Below this a disc's normal is too vertical to say which way is front. */
    private static final double MIN_HORIZONTAL_NORMAL = 0.3;

    private PortalApproach() {
    }

    /** Feet position and view direction in front of {@code portal}, looking at its centre. */
    public static Pos standingBefore(Portal portal, Point reference) {
        PortalLabel label = portal.label();
        Point direction = label != null && label.position() != null ? label.position() : reference;
        return switch (portal.shape()) {
            case Box box -> beforeBox(box, direction);
            case Disc disc -> beforeDisc(disc, direction);
        };
    }

    private static Pos beforeBox(Box box, Point direction) {
        double minX = box.min().x();
        double maxX = box.max().x() + 1;
        double minZ = box.min().z();
        double maxZ = box.max().z() + 1;
        double centreX = (minX + maxX) / 2;
        double centreZ = (minZ + maxZ) / 2;
        double towardsX = direction.x() - centreX;
        double towardsZ = direction.z() - centreZ;
        double feetY = box.min().y();
        Vec centre = new Vec(centreX, (feetY + box.max().y() + 1) / 2, centreZ);
        // Normalised by the half extents so a wide box is not approached over its short side.
        if (Math.abs(towardsX) / ((maxX - minX) / 2) > Math.abs(towardsZ) / ((maxZ - minZ) / 2)) {
            double x = towardsX > 0 ? maxX + CLEARANCE : minX - CLEARANCE;
            return facing(new Vec(x, feetY, centreZ), centre);
        }
        // A tie, including no direction at all, goes south.
        double z = towardsZ < 0 ? minZ - CLEARANCE : maxZ + CLEARANCE;
        return facing(new Vec(centreX, feetY, z), centre);
    }

    private static Pos beforeDisc(Disc disc, Point direction) {
        Vec centre = disc.center();
        Vec towards = new Vec(direction.x() - centre.x(), 0, direction.z() - centre.z());
        Vec normal = disc.normal();
        double horizontal = Math.hypot(normal.x(), normal.z());
        double feetY = centre.y() - disc.radius();
        Vec front;
        double distance;
        if (horizontal >= MIN_HORIZONTAL_NORMAL) {
            Vec side = new Vec(normal.x() / horizontal, 0, normal.z() / horizontal);
            front = side.dot(towards) < 0 ? side.neg() : side;
            // Measured along the normal, so a tilted disc is cleared as far as an upright one.
            distance = CLEARANCE / horizontal;
        } else {
            front = towards.length() > 0 ? towards.normalize() : new Vec(0, 0, 1);
            distance = disc.radius() + CLEARANCE;
        }
        return facing(new Vec(centre.x() + front.x() * distance, feetY, centre.z() + front.z() * distance), centre);
    }

    private static Pos facing(Vec feet, Vec target) {
        Pos eyes = new Pos(feet.x(), feet.y() + EYE_HEIGHT, feet.z()).withLookAt(target);
        return new Pos(feet.x(), feet.y(), feet.z(), eyes.yaw(), eyes.pitch());
    }
}
