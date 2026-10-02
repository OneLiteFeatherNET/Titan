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

import java.util.random.RandomGenerator;

/** How the main heading of a scored course follows its steps. */
@FunctionalInterface
interface Steering {

    /**
     * The main heading after {@code step} led to the block cell {@code placed}, which is the
     * {@code count}-th block of the scored phase.
     */
    Heading follow(Heading heading, Direction step, BlockPos placed, int count);

    /** The heading only bends towards every step: the course keeps its direction. */
    static Steering none() {
        return (heading, step, placed, count) -> heading.steered(step);
    }

    /**
     * Snakes around the spawn. The draw order is part of the contract, so a seed always gives the
     * same course: first the sense of rotation, then the pendulum phase.
     */
    static Steering around(SpawnZone spawn, RandomGenerator random) {
        boolean clockwise = random.nextBoolean();
        double phase = random.nextDouble() * 2.0 * Math.PI;
        return new Around(spawn, clockwise, phase);
    }

    /**
     * Leads around the spawn instead of only away from it. The wish is the tangent of a circle
     * around the spawn, pushed back into a ring by a radial correction and swung to both sides by
     * a pendulum, so the course snakes. A pure value: {@code clockwise} (the sense on the x/z
     * plane, whichever way the axes face) and {@code phase} are fixed per run.
     */
    record Around(SpawnZone spawn, boolean clockwise, double phase) implements Steering {

        /** Closer to the spawn than this, the radial correction pulls outward. */
        static final double INNER_PULL = 24.0;

        /** Further from the spawn than this, the radial correction pulls inward. */
        static final double OUTER_PULL = 56.0;

        /** The ring the course keeps to: the pull has full strength at its edges. */
        static final double RING_MIN = 20.0;

        static final double RING_MAX = 60.0;

        /** The pendulum swings the wish this far to either side. */
        static final double PENDULUM_AMPLITUDE = Math.toRadians(50.0);

        /** Blocks placed per full pendulum swing. */
        static final int PENDULUM_PERIOD = 14;

        @Override
        public Heading follow(Heading heading, Direction step, BlockPos placed, int count) {
            return heading.pulledTowards(wish(placed, count));
        }

        /**
         * Where a course that stands at the block cell {@code pos} after {@code count} blocks wants
         * to go.
         */
        Heading wish(BlockPos pos, int count) {
            double offsetX = pos.x() + 0.5 - spawn.x();
            double offsetZ = pos.z() + 0.5 - spawn.z();
            double distance = Math.hypot(offsetX, offsetZ);
            double outwardX = distance > 0.0 ? offsetX / distance : 1.0;
            double outwardZ = distance > 0.0 ? offsetZ / distance : 0.0;
            double side = clockwise ? 1.0 : -1.0;
            double pull = radialPull(distance);
            double baseX = -side * outwardZ + pull * outwardX;
            double baseZ = side * outwardX + pull * outwardZ;
            double swing = swing(count);
            double wishX = baseX * Math.cos(swing) - baseZ * Math.sin(swing);
            double wishZ = baseX * Math.sin(swing) + baseZ * Math.cos(swing);
            double length = Math.hypot(wishX, wishZ);
            return new Heading(wishX / length, wishZ / length);
        }

        /** The pendulum angle in radians after {@code count} blocks. */
        double swing(int count) {
            return PENDULUM_AMPLITUDE * Math.sin(2.0 * Math.PI * count / PENDULUM_PERIOD + phase);
        }

        /**
         * Outward share in {@code [-1, 1]}: positive below the ring, negative above it, zero in
         * between.
         */
        static double radialPull(double distance) {
            if (distance < INNER_PULL) {
                return Math.min(1.0, (INNER_PULL - distance) / (INNER_PULL - RING_MIN));
            }
            if (distance > OUTER_PULL) {
                return -Math.min(1.0, (distance - OUTER_PULL) / (RING_MAX - OUTER_PULL));
            }
            return 0.0;
        }
    }
}
