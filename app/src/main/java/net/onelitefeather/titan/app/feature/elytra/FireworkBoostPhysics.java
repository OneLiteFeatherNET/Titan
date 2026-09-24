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
package net.onelitefeather.titan.app.feature.elytra;

import java.util.random.RandomGenerator;
import net.minestom.server.coordinate.Vec;

/**
 * Pure vanilla-like elytra firework boost math (see {@code
 * net.minecraft.world.entity.projectile.FireworkRocketEntity#tick} for the formulas this
 * reproduces), kept free of anything server-shaped (no {@code Player}, no scheduler) so it can be
 * unit tested with known inputs and a seeded {@link RandomGenerator} - F.I.R.S.T., no server
 * needed.
 *
 * <p>{@link FireworkBoostTracker} is the only caller: it owns the per-player state and the
 * per-tick scheduling this class has no opinion about.
 */
final class FireworkBoostPhysics {

    /** Vanilla per-tick look acceleration (deltaMovement units). */
    static final double LOOK_ACCELERATION = 0.1;

    /** Vanilla per-tick look target multiplier. */
    static final double LOOK_TARGET = 1.5;

    /** Vanilla per-tick pull-towards-target factor. */
    static final double PULL_TOWARDS_TARGET = 0.5;

    private FireworkBoostPhysics() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * One vanilla tick of the firework boost: nudges {@code velocity} towards
     * {@code look * LOOK_TARGET}. Both the input and the result are in vanilla per-tick units,
     * not yet converted to Minestom's per-second velocity or scaled by the configured boost
     * multiplier - see {@link #toAppliedVelocity(Vec, double, double)}.
     *
     * @param velocity the boosted player's current velocity, in vanilla per-tick units
     * @param look     the direction the player is looking
     * @return the next velocity, in the same units
     */
    static Vec nextVelocity(Vec velocity, Vec look) {
        return velocity.add(look.x() * LOOK_ACCELERATION + (look.x() * LOOK_TARGET - velocity.x()) * PULL_TOWARDS_TARGET, look.y() * LOOK_ACCELERATION + (look.y() * LOOK_TARGET - velocity.y()) * PULL_TOWARDS_TARGET, look.z() * LOOK_ACCELERATION + (look.z() * LOOK_TARGET - velocity.z()) * PULL_TOWARDS_TARGET);
    }

    /**
     * Converts a Minestom per-second velocity (e.g. {@code Player#getVelocity()}) into vanilla
     * per-tick units, the inverse of {@link #toAppliedVelocity(Vec, double, double)} without the
     * boost multiplier - used to seed a new boost from the player's current velocity.
     *
     * @param velocity       a per-second velocity
     * @param ticksPerSecond the server's ticks-per-second (see
     *                       {@link net.minestom.server.ServerFlag#SERVER_TICKS_PER_SECOND})
     * @return the equivalent per-tick velocity
     */
    static Vec toPerTickVelocity(Vec velocity, double ticksPerSecond) {
        return velocity.div(ticksPerSecond);
    }

    /**
     * Converts a vanilla per-tick velocity to the per-second velocity
     * {@code Player#setVelocity(Vec)} expects, scaled by the configured boost multiplier
     * ({@code 1.0} = vanilla).
     *
     * @param velocity        a per-tick velocity, as returned by
     *                        {@link #nextVelocity(Vec, Vec)}
     * @param ticksPerSecond  the server's ticks-per-second
     * @param boostMultiplier the configured {@link ElytraConfig#boostMultiplier()}
     * @return the velocity to apply to the player
     */
    static Vec toAppliedVelocity(Vec velocity, double ticksPerSecond, double boostMultiplier) {
        return velocity.mul(ticksPerSecond * boostMultiplier);
    }

    /**
     * Vanilla's firework lifetime, in ticks: {@code 10 * flightCount + random(6) + random(7)}.
     *
     * @param flightCount the firework's flight-duration data component plus one (a firework with
     *                    no {@code Fireworks} component counts as {@code 1})
     * @param random      the random source; inject a seeded one in tests for a reproducible
     *                    result
     * @return the lifetime, in ticks
     */
    static int lifetime(int flightCount, RandomGenerator random) {
        return 10 * flightCount + random.nextInt(6) + random.nextInt(7);
    }
}
