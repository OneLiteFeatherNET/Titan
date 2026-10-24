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
package net.onelitefeather.titan.feature.throwing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

/**
 * Predicts where a throw lands, for the preview the thrower sees. The model is the one in the
 * design: per tick the velocity is multiplied by 0.98 and lowered by 0.08, then added to the
 * position. It approximates the client's physics, so it is a guide, not a promise.
 */
final class ThrowTrajectory {

    static final int MAX_POINTS = 40;
    static final double DRAG = 0.98;
    static final double GRAVITY = 0.08;
    private static final double TICKS_PER_SECOND = 20.0;

    private ThrowTrajectory() {
    }

    /**
     * @param origin   where the preview starts, the thrower's eyes
     * @param velocity the impulse in blocks per second
     * @param solid    whether a point lies inside a solid block
     * @param points   the free points, at most {@link #MAX_POINTS}
     * @param landing  the last free point before a solid block, if one is hit in reach
     */
    record Trajectory(List<Vec> points, Optional<Vec> landing) {

        Trajectory {
            points = List.copyOf(points);
        }
    }

    static Trajectory compute(Point origin, Vec velocity, Predicate<Point> solid) {
        Vec perTick = velocity.div(TICKS_PER_SECOND);
        Vec position = new Vec(origin.x(), origin.y(), origin.z());
        List<Vec> points = new ArrayList<>();
        Vec landing = null;
        for (int step = 0; step < MAX_POINTS; step++) {
            perTick = perTick.mul(DRAG).sub(0, GRAVITY, 0);
            Vec next = position.add(perTick);
            if (solid.test(next)) {
                landing = position;
                break;
            }
            position = next;
            points.add(position);
        }
        return new Trajectory(points, Optional.ofNullable(landing));
    }
}
