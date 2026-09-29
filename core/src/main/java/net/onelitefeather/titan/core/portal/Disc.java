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
import net.minestom.server.coordinate.Vec;

/**
 * A flat circle of the given radius (rim inclusive) in the plane through {@code center} with the
 * given normal.
 *
 * <p>The segment/plane test is Voyager's {@code RingPass}: intersecting the step between two ticks
 * with the plane, rather than sampling positions, is what keeps a fast elytra step from skipping a
 * one-block-thin disc.
 */
public record Disc(Vec center, double radius, Vec normal) implements PortalShape {

    /** How close to zero the plane/step dot product may be before the step counts as parallel. */
    private static final double PARALLEL_EPSILON = 1e-8;

    /**
     * The normal as a unit vector; hand-written data need not be one. A zero-length normal is
     * returned as is, since rejecting it is the validator's job.
     */
    @Override
    public Vec normal() {
        return normal.length() > 0 ? normal.normalize() : normal;
    }

    @Override
    public boolean crossedBy(Point from, Point to) {
        Vec unitNormal = normal();
        Vec step = new Vec(to.x() - from.x(), to.y() - from.y(), to.z() - from.z());
        double denominator = unitNormal.dot(step);
        if (Math.abs(denominator) < PARALLEL_EPSILON) {
            return false;
        }
        double t = unitNormal.dot(new Vec(center.x() - from.x(), center.y() - from.y(), center.z() - from.z())) / denominator;
        if (t < 0.0 || t > 1.0) {
            return false;
        }
        Vec hit = new Vec(from.x() + step.x() * t, from.y() + step.y() * t, from.z() + step.z() * t);
        // The rim is inclusive and gets no tolerance: one would quietly enlarge every disc.
        return hit.distance(center) <= radius;
    }

    @Override
    public HorizontalBounds horizontalBounds() {
        return new HorizontalBounds(center.x() - radius, center.z() - radius, center.x() + radius, center.z() + radius);
    }
}
