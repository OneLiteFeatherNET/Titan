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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * Reads a standard MIDI file with {@code javax.sound.midi} and keeps its note-ons. Nothing is
 * played here: no synthesizer and no sequencer are opened, so the read needs no audio device.
 */
final class MidiSongParser {

    private static final int CHANNELS = 16;
    /** The meta event type of a tempo change: three bytes of microseconds per quarter note. */
    private static final int TEMPO_META_TYPE = 0x51;
    /** The control change of the channel volume and of the expression; both start at full scale. */
    private static final int CHANNEL_VOLUME = 7;
    private static final int EXPRESSION = 11;
    private static final int FULL_SCALE = 127;

    private MidiSongParser() {
    }

    /**
     * The notes of {@code data} in game ticks, sorted by tick. There is no size or length limit.
     *
     * @throws SongFormatException if the bytes are no playable MIDI file or have no notes
     */
    static List<MidiNote> notes(byte[] data) throws SongFormatException {
        Sequence sequence = read(data);
        if (sequence.getDivisionType() != Sequence.PPQ) {
            throw new SongFormatException("SMPTE timing is not supported");
        }
        List<NoteMapper.RawNote> notes = collect(sequence);
        if (notes.isEmpty()) {
            throw new SongFormatException("the file has no notes");
        }
        return NoteMapper.map(notes);
    }

    private static Sequence read(byte[] data) throws SongFormatException {
        try {
            return MidiSystem.getSequence(new ByteArrayInputStream(data));
        } catch (InvalidMidiDataException | IOException | RuntimeException e) {
            throw new SongFormatException("not a MIDI file");
        }
    }

    // Every track's events in one order by tick. The program of a channel is the last program change before the note.
    // The velocity of a note is scaled by the channel volume and expression set before it; a note that scales to 0 is dropped.
    private static List<NoteMapper.RawNote> collect(Sequence sequence) throws SongFormatException {
        List<TimedMessage> events = new ArrayList<>();
        for (Track track : sequence.getTracks()) {
            for (int index = 0; index < track.size(); index++) {
                MidiEvent event = track.get(index);
                events.add(new TimedMessage(event.getTick(), event.getMessage()));
            }
        }
        events.sort(Comparator.comparingLong(TimedMessage::tick));
        TempoMap tempo = new TempoMap(events, sequence.getResolution());
        int[] programs = new int[CHANNELS];
        int[] volumes = filled(CHANNELS, FULL_SCALE);
        int[] expressions = filled(CHANNELS, FULL_SCALE);
        List<NoteMapper.RawNote> notes = new ArrayList<>();
        for (TimedMessage event : events) {
            if (!(event.message() instanceof ShortMessage message)) {
                continue;
            }
            int channel = message.getChannel();
            if (message.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                programs[channel] = message.getData1();
            } else if (message.getCommand() == ShortMessage.CONTROL_CHANGE) {
                if (message.getData1() == CHANNEL_VOLUME) {
                    volumes[channel] = message.getData2();
                } else if (message.getData1() == EXPRESSION) {
                    expressions[channel] = message.getData2();
                }
            } else if (message.getCommand() == ShortMessage.NOTE_ON && message.getData2() > 0) {
                int velocity = scaledVelocity(message.getData2(), volumes[channel], expressions[channel]);
                if (velocity > 0) {
                    notes.add(new NoteMapper.RawNote(tempo.micros(event.tick()), channel, programs[channel], message.getData1(), velocity));
                }
            }
        }
        return notes;
    }

    /**
     * The velocity times the channel volume and the expression, each over 127, rounded to the
     * nearest whole number.
     */
    private static int scaledVelocity(int velocity, int volume, int expression) {
        return (int) Math.round(velocity * (volume / (double) FULL_SCALE) * (expression / (double) FULL_SCALE));
    }

    private static int[] filled(int size, int value) {
        int[] values = new int[size];
        Arrays.fill(values, value);
        return values;
    }

    private record TimedMessage(long tick, MidiMessage message) {
    }

    /**
     * Converts ticks to microseconds across tempo changes. Each segment starts where the tempo
     * changes and is found by binary search, so a long song with many changes stays cheap.
     */
    private static final class TempoMap {

        private static final int DEFAULT_MICROS_PER_QUARTER = 500_000;

        private final long[] startTicks;
        private final double[] startMicros;
        private final int[] microsPerQuarter;
        private final int resolution;

        TempoMap(List<TimedMessage> events, int resolution) {
            this.resolution = resolution;
            Map<Long, Integer> changes = new TreeMap<>();
            changes.put(0L, DEFAULT_MICROS_PER_QUARTER);
            for (TimedMessage event : events) {
                if (event.message() instanceof MetaMessage meta && meta.getType() == TEMPO_META_TYPE && meta.getData().length == 3) {
                    byte[] data = meta.getData();
                    changes.put(event.tick(), ((data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF));
                }
            }
            int size = changes.size();
            this.startTicks = new long[size];
            this.startMicros = new double[size];
            this.microsPerQuarter = new int[size];
            int index = 0;
            for (Map.Entry<Long, Integer> change : changes.entrySet()) {
                if (index > 0) {
                    int previous = index - 1;
                    this.startMicros[index] = this.startMicros[previous] + (change.getKey() - this.startTicks[previous]) * (double) this.microsPerQuarter[previous] / resolution;
                }
                this.startTicks[index] = change.getKey();
                this.microsPerQuarter[index] = change.getValue();
                index++;
            }
        }

        long micros(long tick) {
            int segment = 0;
            int low = 0;
            int high = this.startTicks.length - 1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                if (this.startTicks[middle] <= tick) {
                    segment = middle;
                    low = middle + 1;
                } else {
                    high = middle - 1;
                }
            }
            double micros = this.startMicros[segment] + (tick - this.startTicks[segment]) * (double) this.microsPerQuarter[segment] / this.resolution;
            return Math.round(micros);
        }
    }
}
