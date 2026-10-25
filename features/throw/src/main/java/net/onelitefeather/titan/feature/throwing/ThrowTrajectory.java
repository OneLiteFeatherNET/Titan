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
 * The flight of a thrown player, the one model behind both the preview the thrower sees and the
 * reach the balance is tested against. Per tick the player moves by the velocity, then the velocity
 * decays: horizontally by {@link #DRAG_HORIZONTAL}, vertically by gravity and
 * {@link #DRAG_VERTICAL}.
 * These are the air resistances Minestom gives a player, which the client applies the same way.
 */
final class ThrowTrajectory {

    static final int MAX_POINTS = 40;
    static final double DRAG_HORIZONTAL = 0.91;
    static final double DRAG_VERTICAL = 0.98;
    static final double GRAVITY = 0.08;
    private static final double TICKS_PER_SECOND = 20.0;
    private static final int MAX_RANGE_TICKS = 400;

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
            Vec next = position.add(perTick);
            if (solid.test(next)) {
                landing = position;
                break;
            }
            position = next;
            points.add(position);
            perTick = nextVelocity(perTick);
        }
        return new Trajectory(points, Optional.ofNullable(landing));
    }

    /**
     * The horizontal distance from the throw point to where the flight comes down to the height it
     * started at, on flat ground, interpolated within the last tick.
     *
     * @param velocity the impulse in blocks per second
     * @return the reach in blocks, or the horizontal distance flown if the flight never lands
     */
    static double rangeOnFlatGround(Vec velocity) {
        Vec perTick = velocity.div(TICKS_PER_SECOND);
        Vec position = Vec.ZERO;
        for (int step = 0; step < MAX_RANGE_TICKS; step++) {
            Vec next = position.add(perTick);
            if (position.y() > 0 && next.y() <= 0) {
                double fraction = position.y() / (position.y() - next.y());
                Vec landing = position.add(perTick.mul(fraction));
                return Math.hypot(landing.x(), landing.z());
            }
            position = next;
            perTick = nextVelocity(perTick);
        }
        return Math.hypot(position.x(), position.z());
    }

    /** The velocity after one tick of the flight: the drag and the gravity of the model. */
    static Vec nextVelocity(Vec perTick) {
        return new Vec(perTick.x() * DRAG_HORIZONTAL, (perTick.y() - GRAVITY) * DRAG_VERTICAL, perTick.z() * DRAG_HORIZONTAL);
    }
}
