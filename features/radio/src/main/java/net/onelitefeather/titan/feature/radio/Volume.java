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
package net.onelitefeather.titan.feature.radio;

import java.util.List;

/**
 * The radio volume as a fixed step list. There is no zero: stop covers silence. The edges hold
 * their value instead of wrapping, so a click at 100 percent never turns into a quiet step.
 */
final class Volume {

    static final List<Integer> STEPS = List.of(25, 50, 75, 100);
    static final int DEFAULT = 100;

    private Volume() {
    }

    /** One step louder; the top step stays. */
    static int up(int percent) {
        for (int step : STEPS) {
            if (step > percent) {
                return step;
            }
        }
        return STEPS.getLast();
    }

    /** One step quieter; the bottom step stays. */
    static int down(int percent) {
        for (int index = STEPS.size() - 1; index >= 0; index--) {
            if (STEPS.get(index) < percent) {
                return STEPS.get(index);
            }
        }
        return STEPS.getFirst();
    }

    /** The factor the sound and the note velocities are scaled by. */
    static float factor(int percent) {
        return percent / 100.0f;
    }
}
