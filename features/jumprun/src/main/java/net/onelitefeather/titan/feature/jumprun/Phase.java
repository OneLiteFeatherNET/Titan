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

import java.util.List;
import java.util.stream.IntStream;

/** How the next jump of a course is generated: first an easy ascent, then by score. */
sealed interface Phase {

    int ASCENT_JUMPS = 5;

    static Phase start(Heading heading) {
        return new Ascent(ASCENT_JUMPS, heading);
    }

    /** The phase for the jump after this one. */
    Phase next();

    /** Air blocks between the blocks that a jump of this phase may leave. */
    IntStream gaps();

    /** Block heights, relative to the source block, that a jump of this phase may rise by. */
    IntStream rises();

    /** The surfaces a block of this phase may have. */
    List<Surface> surfaces();

    /** Easy jumps upward and away from the spawn; they do not count towards the score. */
    record Ascent(int remaining, Heading heading) implements Phase {

        private static final int MAX_GAP = 2;

        @Override
        public Phase next() {
            return remaining <= 1 ? new Scored(0) : new Ascent(remaining - 1, heading);
        }

        @Override
        public IntStream gaps() {
            return IntStream.rangeClosed(Jump.MIN_GAP, MAX_GAP);
        }

        @Override
        public IntStream rises() {
            return IntStream.of(1);
        }

        @Override
        public List<Surface> surfaces() {
            return List.of(Surface.FULL);
        }
    }

    /** Jumps whose difficulty follows {@code score}, the number of scored jumps before this one. */
    record Scored(int score) implements Phase {
        @Override
        public Phase next() {
            return new Scored(score + 1);
        }

        @Override
        public IntStream gaps() {
            return IntStream.rangeClosed(Jump.MIN_GAP, Jump.MAX_GAP);
        }

        @Override
        public IntStream rises() {
            return IntStream.rangeClosed(-1, 1);
        }

        @Override
        public List<Surface> surfaces() {
            return List.of(Surface.values());
        }
    }
}
