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
import java.util.List;

/**
 * Turns the note-on events of a song into {@link MidiNote}s: the tick on the 50 ms grid, the
 * instrument, and a note block note. The melodic part of a song is transposed once, by whole
 * octaves, so that its median lies in the middle of the range; every note is then folded into the
 * range by octaves.
 */
final class NoteMapper {

    /** Microseconds of one server tick. */
    static final long MICROS_PER_TICK = 50_000;

    /** A note-on event as the MIDI file states it. */
    record RawNote(long micros, int channel, int program, int key, int velocity) {
    }

    private NoteMapper() {
    }

    static List<MidiNote> map(List<RawNote> notes) {
        int shift = transposition(notes);
        List<MidiNote> mapped = new ArrayList<>(notes.size());
        for (RawNote note : notes) {
            boolean drum = note.channel() == InstrumentMapper.DRUM_CHANNEL;
            int semitone = drum ? Pitch.CENTRE : Pitch.fold(note.key() - Pitch.LOWEST_KEY + shift);
            mapped.add(new MidiNote(tick(note.micros()), InstrumentMapper.of(note.channel(), note.program(), note.key()), semitone, note.velocity()));
        }
        mapped.sort(Comparator.comparingInt(MidiNote::tick));
        return List.copyOf(mapped);
    }

    /** The tick nearest to {@code micros}; half a tick rounds up. */
    static int tick(long micros) {
        return Math.toIntExact((micros + MICROS_PER_TICK / 2) / MICROS_PER_TICK);
    }

    // The melodic median, lower of the two in an even count, decides the shift; no melodic note means no shift.
    private static int transposition(List<RawNote> notes) {
        List<Integer> melodic = notes.stream().filter(note -> note.channel() != InstrumentMapper.DRUM_CHANNEL).map(note -> note.key() - Pitch.LOWEST_KEY).sorted().toList();
        if (melodic.isEmpty()) {
            return 0;
        }
        return Pitch.transposition(melodic.get((melodic.size() - 1) / 2));
    }
}
