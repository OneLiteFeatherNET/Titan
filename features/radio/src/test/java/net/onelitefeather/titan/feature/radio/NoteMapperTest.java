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

import java.util.ArrayList;
import java.util.Collections;
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

    /** A melodic track on the harp: one note per tick, keys in order. */
    private static List<NoteMapper.RawNote> phrase(int... keys) {
        List<NoteMapper.RawNote> notes = new ArrayList<>();
        for (int tick = 0; tick < keys.length; tick++) {
            notes.add(melody((long) tick * MICROS_PER_TICK, keys[tick], 100));
        }
        return notes;
    }

    /** The MIDI key a note sounds as: its instrument's base key plus its note. */
    private static int soundingKey(MidiNote note) {
        return note.instrument().baseKey() + note.semitone();
    }

    private static List<Integer> soundingKeys(List<MidiNote> notes) {
        return notes.stream().map(NoteMapperTest::soundingKey).toList();
    }

    private static List<RadioInstrument> instruments(List<MidiNote> notes) {
        return notes.stream().map(MidiNote::instrument).toList();
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

    @DisplayName("A melody that climbs past the harp register moves the whole track down an octave and stays on the harp")
    @Test
    void melodyAboveTheHarpMovesTheTrackDown() {
        List<MidiNote> notes = NoteMapper.map(phrase(66, 79));

        Assertions.assertEquals(List.of(RadioInstrument.HARP, RadioInstrument.HARP), instruments(notes), "both notes must stay on the harp");
        Assertions.assertEquals(List.of(54, 67), soundingKeys(notes), "the track must sound one octave below the written keys 66 and 79");
    }

    @DisplayName("A melodic track above the harp register plays entirely on the harp, one octave lower")
    @Test
    void korobeikiLikeMelodyPlaysOnTheHarpOneOctaveLower() {
        List<MidiNote> notes = NoteMapper.map(phrase(72, 76, 77, 79, 81, 83, 84, 86, 88));

        Assertions.assertEquals(Collections.nCopies(9, RadioInstrument.HARP), instruments(notes), "every note of the melody must stay on the harp");
        Assertions.assertEquals(List.of(60, 64, 65, 67, 69, 71, 72, 74, 76), soundingKeys(notes), "the melody must sound one octave below the written keys C5 to E6");
    }

    @DisplayName("A track spanning three octaves keeps its intervals and only its outliers change instrument")
    @Test
    void threeOctaveTrackKeepsItsIntervals() {
        List<MidiNote> notes = NoteMapper.map(phrase(48, 60, 72, 84));

        Assertions.assertEquals(List.of(RadioInstrument.GUITAR, RadioInstrument.HARP, RadioInstrument.HARP, RadioInstrument.FLUTE), instruments(notes), "keys 60 and 72 must stay on the harp, the outliers 48 and 84 must switch to the extensions");
        Assertions.assertEquals(List.of(48, 60, 72, 84), soundingKeys(notes), "no shift: every note must sound as written");
    }

    @DisplayName("A bass track keeps its octave while the melody above it moves down")
    @Test
    void bassTrackKeepsItsOctaveBesideAMelody() {
        List<NoteMapper.RawNote> notes = new ArrayList<>(phrase(72, 76, 79, 84, 88));
        notes.add(new NoteMapper.RawNote(0, BASS_CHANNEL, BASS_PROGRAM, 42, 100));

        List<MidiNote> mapped = NoteMapper.map(notes);

        Assertions.assertEquals(List.of(42), mapped.stream().filter(note -> note.instrument() == RadioInstrument.BASS).map(NoteMapperTest::soundingKey).toList(), "the bass note must sound as key 42, unshifted");
        Assertions.assertEquals(Collections.nCopies(5, RadioInstrument.HARP), mapped.stream().filter(note -> note.instrument() != RadioInstrument.BASS).map(MidiNote::instrument).toList(), "the melody must stay on the harp");
    }

    @DisplayName("When plus and minus shift a track equally, the shift whose median sits nearer the register centre wins")
    @Test
    void tieBetweenPlusAndMinusFollowsTheMedian() {
        List<MidiNote> notes = NoteMapper.map(phrase(20, 43, 85));

        Assertions.assertEquals(RadioInstrument.HARP, notes.get(1).instrument(), "key 43 must play on the harp after the shift");
        Assertions.assertEquals(55, soundingKey(notes.get(1)), "the shift must be +12: the median 43 sits nearer the centre 66 at 55 than at 31");
    }

    @DisplayName("The shift of a track does not depend on the order its notes arrive in")
    @Test
    void shiftDoesNotDependOnNoteOrder() {
        List<NoteMapper.RawNote> notes = phrase(20, 43, 85);
        List<NoteMapper.RawNote> reversed = new ArrayList<>(notes);
        Collections.reverse(reversed);

        Assertions.assertEquals(NoteMapper.map(notes), NoteMapper.map(reversed), "the same notes must map to the same instruments and notes in any order");
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
