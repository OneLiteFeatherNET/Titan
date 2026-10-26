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

/**
 * The note block scale: notes 0 to 24 are two octaves, from F#3 (MIDI key 54) to F#5. A note's
 * sound pitch is {@code 2^((n - 12) / 12)}.
 */
final class Pitch {

    static final int LOWEST = 0;
    static final int CENTRE = 12;
    static final int HIGHEST = 24;
    /** The MIDI key of note 0. */
    static final int LOWEST_KEY = 54;
    private static final int OCTAVE = 12;

    private Pitch() {
    }

    /** The sound pitch of note {@code semitone}: 0.5 at note 0, 1.0 at note 12, 2.0 at note 24. */
    static float of(int semitone) {
        return (float) Math.pow(2, (semitone - CENTRE) / (double) OCTAVE);
    }

    /**
     * The shift, a whole number of octaves, that moves {@code median} onto the centre of the range.
     * A median exactly halfway between two octaves rounds down.
     */
    static int transposition(int median) {
        int distance = CENTRE - median;
        // ceil(distance / 12 - 1/2) in whole numbers: the tie at .5 falls to the lower octave.
        int octaves = -Math.floorDiv(-(2 * distance - OCTAVE), 2 * OCTAVE);
        return octaves * OCTAVE;
    }

    /** Moves {@code semitone} by octaves until it lies in the range; the pitch class stays. */
    static int fold(int semitone) {
        int folded = semitone;
        while (folded < LOWEST) {
            folded += OCTAVE;
        }
        while (folded > HIGHEST) {
            folded -= OCTAVE;
        }
        return folded;
    }
}
