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
    private static final int BASS_CHANNEL = 1;
    private static final int BASS_PROGRAM = 32;

    private static NoteMapper.RawNote melody(long micros, int key, int velocity) {
        return new NoteMapper.RawNote(micros, MELODY_CHANNEL, 0, key, velocity);
    }

    /** The MIDI key a note sounds as: its instrument's base key plus its note. */
    private static int soundingKey(MidiNote note) {
        return note.instrument().baseKey() + note.semitone();
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

    @DisplayName("A melody note above the harp register moves to the flute and sounds one octave up, not down")
    @Test
    void melodyAboveTheHarpStaysInItsOctave() {
        List<MidiNote> notes = NoteMapper.map(List.of(melody(0, 66, 100), melody(0, 79, 100)));

        Assertions.assertEquals(RadioInstrument.FLUTE, notes.get(1).instrument(), "key 79 must play on the flute, the register nearest to the harp");
        Assertions.assertEquals(13, notes.get(1).semitone(), "key 79 on the flute must be note 13");
        Assertions.assertEquals(79, soundingKey(notes.get(1)), "the note must sound as key 79, not an octave lower");
    }

    @DisplayName("A melody note above the flute register moves to the bell")
    @Test
    void melodyAboveTheFluteMovesToTheBell() {
        List<MidiNote> notes = NoteMapper.map(List.of(melody(0, 66, 100), melody(0, 66, 100), melody(0, 66, 100), melody(0, 96, 100)));

        Assertions.assertEquals(RadioInstrument.BELL, notes.get(3).instrument(), "key 96 must play on the bell");
        Assertions.assertEquals(96, soundingKey(notes.get(3)), "the note must sound as key 96");
    }

    @DisplayName("A bass part plays on the bass in its real octave")
    @Test
    void bassPartPlaysInItsRealOctave() {
        List<NoteMapper.RawNote> notes = List.of(melody(0, 66, 100), melody(0, 66, 100), melody(0, 66, 100), new NoteMapper.RawNote(0, BASS_CHANNEL, BASS_PROGRAM, 42, 100));

        MidiNote bass = NoteMapper.map(notes).stream().filter(note -> note.instrument() == RadioInstrument.BASS).findFirst().orElseThrow();
        Assertions.assertEquals(12, bass.semitone(), "key 42 on the bass must be note 12");
        Assertions.assertEquals(42, soundingKey(bass), "the bass note must sound as key 42, its real octave");
    }

    @DisplayName("A melody note below the span is folded up by octaves and keeps its pitch class")
    @Test
    void melodyBelowTheSpanIsFoldedUp() {
        List<MidiNote> notes = NoteMapper.map(List.of(melody(0, 66, 100), melody(0, 66, 100), melody(0, 66, 100), melody(0, 20, 100)));

        Assertions.assertEquals(32, soundingKey(notes.get(3)), "key 20 must fold up to 32");
        Assertions.assertEquals(RadioInstrument.BASS, notes.get(3).instrument(), "key 32 must play on the bass");
    }

    @DisplayName("A melody note above the span is folded down by octaves and keeps its pitch class")
    @Test
    void melodyAboveTheSpanIsFoldedDown() {
        List<MidiNote> notes = NoteMapper.map(List.of(melody(0, 66, 100), melody(0, 66, 100), melody(0, 66, 100), melody(0, 110, 100)));

        Assertions.assertEquals(RadioInstrument.BELL, notes.get(3).instrument(), "key 98 must play on the bell");
        Assertions.assertEquals(20, notes.get(3).semitone(), "key 98 on the bell must be note 20");
    }

    @DisplayName("A bass-only song with median F#2 stays on the bass in its own octave")
    @Test
    void bassOnlySongStaysInItsOctave() {
        List<MidiNote> notes = NoteMapper.map(List.of(new NoteMapper.RawNote(0, BASS_CHANNEL, BASS_PROGRAM, 42, 100), new NoteMapper.RawNote(MICROS_PER_TICK, BASS_CHANNEL, BASS_PROGRAM, 42, 100)));

        Assertions.assertEquals(List.of(RadioInstrument.BASS, RadioInstrument.BASS), notes.stream().map(MidiNote::instrument).toList(), "every note of the bass part must play on the bass");
        Assertions.assertEquals(List.of(12, 12), notes.stream().map(MidiNote::semitone).toList(), "key 42 on the bass must be note 12, no whole-song shift");
        Assertions.assertEquals(List.of(42, 42), notes.stream().map(NoteMapperTest::soundingKey).toList(), "the bass notes must sound as key 42, their real octave");
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
