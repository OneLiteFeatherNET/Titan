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

    /** The ascent lasts at least this many jumps, however open the space below already is. */
    int MIN_ASCENT_JUMPS = 5;

    /** The ascent gives up after this many jumps: the run does not start. */
    int MAX_ASCENT_JUMPS = 30;

    static Phase start(Heading heading) {
        return new Ascent(0, heading);
    }

    /**
     * The main heading of the course: the direction it leads in overall. It starts as the ascent
     * heading and bends towards every step made, so a course keeps its direction.
     */
    Heading heading();

    /** This phase with another main heading. */
    Phase withHeading(Heading heading);

    /** The phase after a step: the same, with the heading bent towards the step. */
    default Phase steered(Direction step) {
        return withHeading(heading().steered(step));
    }

    /** Air blocks between the blocks that a jump of this phase may leave. */
    IntStream gaps();

    /** Block heights, relative to the source block, that a jump of this phase may rise by. */
    IntStream rises();

    /** The surfaces a block of this phase may have. */
    List<Surface> surfaces();

    /**
     * Easy jumps upward and away from the spawn, until a block stands in the open and far from the
     * spawn; they do not count towards the score. {@code jumps} is how many were made so far.
     */
    record Ascent(int jumps, Heading heading) implements Phase {

        private static final int MAX_GAP = 2;

        /** The phase after one more jump, whose block is or is not yet in the open. */
        Phase next(boolean inTheOpen) {
            int made = jumps + 1;
            return made >= MIN_ASCENT_JUMPS && inTheOpen ? new Scored(0, heading) : new Ascent(made, heading);
        }

        @Override
        public Phase withHeading(Heading heading) {
            return new Ascent(jumps, heading);
        }

        boolean isOutOfJumps() {
            return jumps >= MAX_ASCENT_JUMPS;
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

    /**
     * Jumps whose difficulty follows {@code score}, the number of scored jumps before this one.
     * Only the shapes unlocked at that score appear.
     */
    record Scored(int score, Heading heading) implements Phase {
        Phase next() {
            return new Scored(score + 1, heading);
        }

        @Override
        public Phase withHeading(Heading heading) {
            return new Scored(score, heading);
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
            return Surface.unlockedAt(score);
        }
    }
}
