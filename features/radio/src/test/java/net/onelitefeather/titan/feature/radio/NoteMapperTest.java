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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoteMapperTest {

    private static final int MELODY_CHANNEL = 0;
    private static final int DRUM_CHANNEL = 9;
    private static final int MICROS_PER_TICK = 50_000;

    private static NoteMapper.RawNote melody(long micros, int key, int velocity) {
        return new NoteMapper.RawNote(micros, MELODY_CHANNEL, 0, key, velocity);
    }

    @DisplayName("A note is placed on the server tick it is nearest to, in microseconds")
    @Test
    void ticksAreRoundedFromMicroseconds() {
        List<MidiNote> notes = NoteMapper.map(List.of(melody(MICROS_PER_TICK / 2, 66, 100), melody(MICROS_PER_TICK / 2 - 1, 66, 100)));

        Assertions.assertEquals(List.of(0, 1), notes.stream().map(MidiNote::tick).toList(), "half a tick rounds up, anything below does not");
    }

    @DisplayName("Notes of the melody that sit at the centre keep their semitone")
    @Test
    void centredMelodyKeepsItsSemitone() {
        List<MidiNote> notes = NoteMapper.map(List.of(melody(0, 66, 100)));

        Assertions.assertEquals(12, notes.getFirst().semitone(), "MIDI key 66 must play as note 12");
        Assertions.assertEquals(100, notes.getFirst().velocity(), "the velocity must be kept for the volume");
    }

    @DisplayName("A melody outside the range is moved by octaves into it")
    @Test
    void melodyOutsideTheRangeIsFolded() {
        List<MidiNote> notes = NoteMapper.map(List.of(melody(0, 66, 100), melody(0, 79, 100)));

        Assertions.assertEquals(List.of(12, 13), notes.stream().map(MidiNote::semitone).toList(), "key 79 is 25 notes above the bottom of the range and must fold down to 13");
    }

    @DisplayName("Drum notes keep their key-based instrument and play at pitch one")
    @Test
    void drumNotesPlayAtPitchOne() {
        List<MidiNote> notes = NoteMapper.map(List.of(new NoteMapper.RawNote(0, DRUM_CHANNEL, 0, 36, 100)));

        Assertions.assertEquals(RadioInstrument.BASEDRUM, notes.getFirst().instrument(), "the bass drum key must play basedrum");
        Assertions.assertEquals(12, notes.getFirst().semitone(), "a drum note must not be transposed or folded");
    }

    @DisplayName("The notes come out sorted by tick")
    @Test
    void notesAreSortedByTick() {
        List<MidiNote> notes = NoteMapper.map(List.of(melody(MICROS_PER_TICK * 3, 66, 100), melody(MICROS_PER_TICK, 66, 100)));

        Assertions.assertEquals(List.of(1, 3), notes.stream().map(MidiNote::tick).toList(), "notes must be ordered by their tick");
    }
}
