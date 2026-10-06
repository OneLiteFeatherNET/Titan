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
package net.onelitefeather.titan.setup.portal.editor;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Disc;

/**
 * Turns where a builder stands and looks into the parts of a {@link Disc}. Pure, so the rounding
 * and
 * snapping rules are testable without a server.
 */
public final class DiscPlacement {

    /** Looking "straight ahead" rarely hits an axis exactly; within this angle the normal snaps. */
    static final double SNAP_DEGREES = 5.0;

    private static final double SNAP_COSINE = Math.cos(Math.toRadians(SNAP_DEGREES));
    private static final Vec[] AXES = {new Vec(1, 0, 0), new Vec(-1, 0, 0), new Vec(0, 1, 0), new Vec(0, -1, 0), new Vec(0, 0, 1), new Vec(0, 0, -1)
    };

    private DiscPlacement() {
    }

    public static Disc of(Point eye, Vec look, double radius) {
        return new Disc(centre(eye), radius, normal(look));
    }

    /** The eye position with each coordinate rounded to the nearest multiple of 0.5. */
    public static Vec centre(Point eye) {
        return new Vec(roundToHalf(eye.x()), roundToHalf(eye.y()), roundToHalf(eye.z()));
    }

    /**
     * The unit look direction, snapped onto the nearest of the six axes when at most
     * {@value #SNAP_DEGREES} degrees away from it.
     *
     * @throws IllegalArgumentException if {@code look} has length zero
     */
    public static Vec normal(Vec look) {
        if (!(look.length() > 0)) {
            throw new IllegalArgumentException("look direction must not have length 0");
        }
        Vec unit = look.normalize();
        for (Vec axis : AXES) {
            if (unit.dot(axis) >= SNAP_COSINE) {
                return axis;
            }
        }
        return unit;
    }

    private static double roundToHalf(double value) {
        return Math.round(value * 2) / 2.0;
    }
}
