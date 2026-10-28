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

import java.util.Objects;

/**
 * One note of a MIDI song on the server tick grid.
 *
 * @param tick       the game tick the note starts on, counted from the start of the song
 * @param instrument the note block instrument that plays it
 * @param semitone   the note block note, {@code 0} to {@link Pitch#HIGHEST}
 * @param velocity   the velocity after channel volume and expression, {@code 1} to {@code 127}
 */
record MidiNote(int tick, RadioInstrument instrument, int semitone, int velocity) {

    MidiNote {
        Objects.requireNonNull(instrument, "instrument must not be null");
        if (tick < 0) {
            throw new IllegalArgumentException("tick must not be negative, was " + tick);
        }
        if (semitone < Pitch.LOWEST || semitone > Pitch.HIGHEST) {
            throw new IllegalArgumentException("semitone must be between " + Pitch.LOWEST + " and " + Pitch.HIGHEST + ", was " + semitone);
        }
        if (velocity < 1 || velocity > 127) {
            throw new IllegalArgumentException("velocity must be between 1 and 127, was " + velocity);
        }
    }

    /** The loudness of the note: its velocity over 127. */
    float volume() {
        return this.velocity / 127f;
    }
}
