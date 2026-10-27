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
 * instrument, and a note block note. A note keeps the octave it has in the MIDI file: a melodic
 * note
 * plays on the instrument whose register holds it, a range extension if its own register does not.
 * Only a key outside the span of all registers is folded by octaves first.
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
        List<MidiNote> mapped = new ArrayList<>(notes.size());
        for (RawNote note : notes) {
            RadioInstrument instrument = InstrumentMapper.of(note.channel(), note.program(), note.key());
            if (note.channel() == InstrumentMapper.DRUM_CHANNEL) {
                mapped.add(new MidiNote(tick(note.micros()), instrument, Pitch.CENTRE, note.velocity()));
            } else {
                int key = Pitch.foldIntoSpan(note.key());
                RadioInstrument played = instrument.playing(key);
                mapped.add(new MidiNote(tick(note.micros()), played, played.semitoneOf(key), note.velocity()));
            }
        }
        mapped.sort(Comparator.comparingInt(MidiNote::tick));
        return List.copyOf(mapped);
    }

    /** The tick nearest to {@code micros}; half a tick rounds up. */
    static int tick(long micros) {
        return Math.toIntExact((micros + MICROS_PER_TICK / 2) / MICROS_PER_TICK);
    }
}
