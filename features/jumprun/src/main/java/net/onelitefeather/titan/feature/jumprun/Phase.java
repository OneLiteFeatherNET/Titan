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

/** How the next jump of a course is generated: first an easy ascent, then by score. */
sealed interface Phase {

    int ASCENT_JUMPS = 5;

    static Phase start(Heading heading) {
        return new Ascent(ASCENT_JUMPS, heading);
    }

    /** The phase for the jump after this one. */
    Phase next();

    /** Easy jumps upward and away from the spawn; they do not count towards the score. */
    record Ascent(int remaining, Heading heading) implements Phase {
        @Override
        public Phase next() {
            return remaining <= 1 ? new Scored(0) : new Ascent(remaining - 1, heading);
        }
    }

    /** Jumps whose difficulty follows {@code score}, the number of scored jumps before this one. */
    record Scored(int score) implements Phase {
        @Override
        public Phase next() {
            return new Scored(score + 1);
        }
    }
}
