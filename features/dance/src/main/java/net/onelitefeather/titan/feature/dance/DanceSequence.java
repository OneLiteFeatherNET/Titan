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
package net.onelitefeather.titan.feature.dance;

import java.util.ArrayList;
import java.util.List;

/**
 * A fixed, finite list of {@link DanceAction}s at tick offsets from the start of a dance. Ticks
 * from {@code 0} up to, not including, {@link #lengthTicks()} carry steps; the dance ends when its
 * length is reached.
 */
final class DanceSequence {

    private final int lengthTicks;
    private final List<DanceStep> steps;

    private DanceSequence(int lengthTicks, List<DanceStep> steps) {
        this.lengthTicks = lengthTicks;
        this.steps = List.copyOf(steps);
    }

    /** @return a builder for a dance that lasts {@code lengthTicks} ticks */
    static Builder lasting(int lengthTicks) {
        return new Builder(lengthTicks);
    }

    int lengthTicks() {
        return this.lengthTicks;
    }

    /** @return the actions scheduled for {@code tick}, in the order they were added */
    List<DanceAction> actionsAt(int tick) {
        return this.steps.stream().filter(step -> step.tick() == tick).map(DanceStep::action).toList();
    }

    /** @return {@code true} once {@code tick} has reached the length, which ends the dance */
    boolean isOver(int tick) {
        return tick >= this.lengthTicks;
    }

    /** Collects the steps of one dance; every step must fall inside the length. */
    static final class Builder {

        private final int lengthTicks;
        private final List<DanceStep> steps = new ArrayList<>();

        private Builder(int lengthTicks) {
            if (lengthTicks <= 0) {
                throw new IllegalArgumentException("a dance must last at least one tick, not " + lengthTicks);
            }
            this.lengthTicks = lengthTicks;
        }

        /** Adds {@code action} once, at {@code tick}. */
        Builder at(int tick, DanceAction action) {
            if (tick < 0 || tick >= this.lengthTicks) {
                throw new IllegalArgumentException("step at tick " + tick + " lies outside the " + this.lengthTicks + " ticks of the dance");
            }
            this.steps.add(new DanceStep(tick, action));
            return this;
        }

        /**
         * Adds {@code action} at {@code from}, {@code from + period}, ... while below
         * {@code until}.
         */
        Builder every(int period, int from, int until, DanceAction action) {
            requirePeriod(period);
            for (int tick = from; tick < until; tick += period) {
                at(tick, action);
            }
            return this;
        }

        /**
         * Like {@link #every}, but alternates {@code first} and {@code second}, starting with
         * {@code first}.
         */
        Builder alternating(int period, int from, int until, DanceAction first, DanceAction second) {
            requirePeriod(period);
            boolean useFirst = true;
            for (int tick = from; tick < until; tick += period) {
                at(tick, useFirst ? first : second);
                useFirst = !useFirst;
            }
            return this;
        }

        DanceSequence build() {
            return new DanceSequence(this.lengthTicks, this.steps);
        }

        private static void requirePeriod(int period) {
            if (period <= 0) {
                throw new IllegalArgumentException("a period must be at least one tick, not " + period);
            }
        }
    }
}
