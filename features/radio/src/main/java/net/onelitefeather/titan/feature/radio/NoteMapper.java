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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the note-on events of a song into {@link MidiNote}s: the tick on the 50 ms grid, the
 * instrument, and a note block note. Each melodic track (channel and program) moves by whole
 * octaves, the shift that puts most of its notes into its instrument's register. A note the
 * register still does not hold plays on a range extension; only a key outside the span of all
 * registers is folded by octaves first.
 */
final class NoteMapper {

    /** Microseconds of one server tick. */
    static final long MICROS_PER_TICK = 50_000;

    /** The whole-octave shifts a track may take, in semitones. */
    private static final List<Integer> SHIFTS = List.of(0, -12, 12, -24, 24, -36, 36, -48, 48);

    /** A note-on event as the MIDI file states it. */
    record RawNote(long micros, int channel, int program, int key, int velocity) {
    }

    /** A melodic track: its channel and program, and the instrument they choose. */
    private record Track(int channel, int program, RadioInstrument instrument) {

        static Track of(RawNote note) {
            return new Track(note.channel(), note.program(), InstrumentMapper.of(note.channel(), note.program(), note.key()));
        }
    }

    private NoteMapper() {
    }

    static List<MidiNote> map(List<RawNote> notes) {
        Map<Track, Integer> shifts = trackShifts(notes);
        List<MidiNote> mapped = new ArrayList<>(notes.size());
        for (RawNote note : notes) {
            RadioInstrument instrument = InstrumentMapper.of(note.channel(), note.program(), note.key());
            if (note.channel() == InstrumentMapper.DRUM_CHANNEL) {
                mapped.add(new MidiNote(tick(note.micros()), instrument, Pitch.CENTRE, note.velocity()));
            } else {
                int key = Pitch.foldIntoSpan(note.key() + shifts.get(Track.of(note)));
                RadioInstrument played = instrument.playing(key);
                mapped.add(new MidiNote(tick(note.micros()), played, played.semitoneOf(key), note.velocity()));
            }
        }
        mapped.sort(Comparator.comparingInt(MidiNote::tick));
        return List.copyOf(mapped);
    }

    /** The shift of every melodic track; drums are not a track and never shift. */
    private static Map<Track, Integer> trackShifts(List<RawNote> notes) {
        Map<Track, List<Integer>> keysByTrack = new HashMap<>();
        for (RawNote note : notes) {
            if (note.channel() != InstrumentMapper.DRUM_CHANNEL) {
                keysByTrack.computeIfAbsent(Track.of(note), track -> new ArrayList<>()).add(note.key());
            }
        }
        Map<Track, Integer> shifts = new HashMap<>();
        keysByTrack.forEach((track, keys) -> shifts.put(track, shiftOf(track.instrument(), keys)));
        return shifts;
    }

    /**
     * The shift that puts the most keys into the register of {@code instrument}. A tie goes to the
     * smaller shift, then to the one whose median lies nearest the register centre, then to the
     * lower shift, so the choice never depends on the order of the notes.
     */
    private static int shiftOf(RadioInstrument instrument, List<Integer> keys) {
        int median = median(keys);
        int centre = instrument.baseKey() + Pitch.CENTRE;
        Comparator<Integer> mostNotesInRegister = Comparator.comparingLong(shift -> -countInRegister(instrument, keys, shift));
        Comparator<Integer> nearestCentre = Comparator.comparingInt(shift -> Math.abs(median + shift - centre));
        return SHIFTS.stream().min(mostNotesInRegister.thenComparingInt(shift -> Math.abs(shift)).thenComparing(nearestCentre).thenComparingInt(shift -> shift)).orElseThrow();
    }

    private static long countInRegister(RadioInstrument instrument, List<Integer> keys, int shift) {
        return keys.stream().filter(key -> instrument.reaches(key + shift)).count();
    }

    /** The middle key of the unshifted track; of two middle keys, the lower one. */
    private static int median(List<Integer> keys) {
        List<Integer> sorted = keys.stream().sorted().toList();
        return sorted.get((sorted.size() - 1) / 2);
    }

    /** The tick nearest to {@code micros}; half a tick rounds up. */
    static int tick(long micros) {
        return Math.toIntExact((micros + MICROS_PER_TICK / 2) / MICROS_PER_TICK);
    }
}
