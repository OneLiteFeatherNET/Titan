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

import net.minestom.server.coordinate.Vec;

/** The impulse a throw gives the thrown player, in blocks per second. Pure. */
final class ThrowVelocity {

    /** The vertical part is capped so a throw cannot launch the player into the roof. */
    static final double MAX_VERTICAL = 10.0;

    private ThrowVelocity() {
    }

    /**
     * The impulse for a throw along {@code look} at {@code strength}. The vertical part is taken as
     * a magnitude, so a steep look down still arcs up: the impulse never points downwards, and its
     * length is the charged speed (unless the look is straight down, which has no horizontal part).
     *
     * @param look     the thrower's look direction
     * @param strength from {@link Charge#strength(long)}
     */
    static Vec impulse(Vec look, double strength) {
        double speed = Charge.speed(strength);
        double length = look.length();
        if (length == 0.0) {
            return Vec.ZERO;
        }
        Vec unit = look.div(length);
        double vertical = Math.min(Math.abs(unit.y()) * speed, MAX_VERTICAL);
        double horizontal = Math.hypot(unit.x(), unit.z());
        if (horizontal == 0.0) {
            return new Vec(0, vertical, 0);
        }
        double horizontalSpeed = Math.sqrt(Math.max(0.0, speed * speed - vertical * vertical));
        double scale = horizontalSpeed / horizontal;
        return new Vec(unit.x() * scale, vertical, unit.z() * scale);
    }
}
