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

/**
 * Turns the ticks a charge was held into the throw's strength and speed. Pure, so the balance is
 * tested without a server.
 */
final class Charge {

    /** Held this long (1.5 seconds) the charge is full; longer holds add nothing. */
    static final int FULL_TICKS = 30;

    /** Shorter holds are taps: they throw with the minimum strength. */
    static final int TAP_TICKS = 2;

    /**
     * Blocks per second of a tap, which lands about six blocks away at 45 degrees; the reach is
     * pinned by {@code ThrowRangeTest} against the flight model of {@link ThrowTrajectory}.
     */
    static final double MIN_SPEED = 18.0;

    /** Blocks per second of a full charge, which lands about twenty blocks away at 45 degrees. */
    static final double MAX_SPEED = 50.0;

    private Charge() {
    }

    /**
     * @param ticks how long the charge item was held
     * @return the strength from 0 to 1
     */
    static double strength(long ticks) {
        if (ticks < TAP_TICKS) {
            return 0.0;
        }
        return Math.min(ticks, FULL_TICKS) / (double) FULL_TICKS;
    }

    /**
     * @param strength from {@link #strength(long)}; values outside 0 to 1 are clamped
     * @return the speed of the throw in blocks per second
     */
    static double speed(double strength) {
        double clamped = Math.max(0.0, Math.min(1.0, strength));
        return MIN_SPEED + (MAX_SPEED - MIN_SPEED) * clamped;
    }
}
