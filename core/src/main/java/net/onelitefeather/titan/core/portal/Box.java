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
 * An axis-parallel box. {@code min} and {@code max} are block coordinates, both inclusive, so the
 * box covers {@code [min, max + 1]} per axis in world coordinates.
 */
public record Box(Vec min, Vec max) implements PortalShape {

    @Override
    public boolean crossedBy(Point from, Point to) {
        double[] window = {0.0, 1.0};
        return narrow(window, from.x(), to.x(), min.x(), max.x() + 1)
                && narrow(window, from.y(), to.y(), min.y(), max.y() + 1)
                && narrow(window, from.z(), to.z(), min.z(), max.z() + 1);
    }

    /**
     * Slab method: shrinks the parameter window {@code [t0, t1]} of the segment to the part that lies
     * between {@code lo} and {@code hi} on one axis. A step without movement on the axis (which
     * includes a zero-length step) can only be inside the slab or not, hence containment.
     */
    private static boolean narrow(double[] window, double from, double to, double lo, double hi) {
        double delta = to - from;
        if (delta == 0.0) {
            return from >= lo && from <= hi;
        }
        double ta = (lo - from) / delta;
        double tb = (hi - from) / delta;
        window[0] = Math.max(window[0], Math.min(ta, tb));
        window[1] = Math.min(window[1], Math.max(ta, tb));
        return window[0] <= window[1];
    }

    @Override
    public HorizontalBounds horizontalBounds() {
        return new HorizontalBounds(min.x(), min.z(), max.x() + 1, max.z() + 1);
    }
}
