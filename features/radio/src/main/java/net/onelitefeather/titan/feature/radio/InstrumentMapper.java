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

import java.util.Arrays;

/**
 * Chooses the note block instrument of a MIDI note from its channel, program and key. Channel ten
 * (index 9) is drums and picks by key; every other channel follows the General MIDI program.
 */
final class InstrumentMapper {

    static final int DRUM_CHANNEL = 9;

    /** The number of values of a MIDI data byte, which are the programs and the keys. */
    private static final int MIDI_VALUES = 128;

    /**
     * A drum key's sound: the instrument and the fixed note it plays, not moved by any register.
     */
    record DrumSound(RadioInstrument instrument, int semitone) {
    }

    /** The instrument of each General MIDI program, by instrument family. */
    private static final RadioInstrument[] PROGRAMS = programTable();

    /**
     * The sound of each drum key on channel ten; a key without an entry plays the hat on note 12.
     */
    private static final DrumSound[] DRUM_KEYS = drumTable();

    private InstrumentMapper() {
    }

    static RadioInstrument of(int channel, int program, int key) {
        if (channel < 0 || channel > 15 || program < 0 || program > 127 || key < 0 || key > 127) {
            throw new IllegalArgumentException("channel, program and key must be MIDI data values, was " + channel + ", " + program + ", " + key);
        }
        return channel == DRUM_CHANNEL ? drum(key).instrument() : PROGRAMS[program];
    }

    /** The sound of a drum key on channel ten. */
    static DrumSound drum(int key) {
        return DRUM_KEYS[key];
    }

    private static RadioInstrument[] programTable() {
        RadioInstrument[] table = new RadioInstrument[MIDI_VALUES];
        fillRange(table, RadioInstrument.HARP, 0, 3);
        fillRange(table, RadioInstrument.PLING, 4, 5);
        set(table, RadioInstrument.GUITAR, 6);
        set(table, RadioInstrument.BANJO, 7);
        fillRange(table, RadioInstrument.BELL, 8, 9);
        set(table, RadioInstrument.CHIME, 10);
        set(table, RadioInstrument.IRON_XYLOPHONE, 11);
        fillRange(table, RadioInstrument.XYLOPHONE, 12, 13);
        set(table, RadioInstrument.CHIME, 14);
        set(table, RadioInstrument.BANJO, 15);

        fillRange(table, RadioInstrument.FLUTE, 16, 23);
        fillRange(table, RadioInstrument.GUITAR, 24, 28);
        set(table, RadioInstrument.BIT, 29, 30);
        set(table, RadioInstrument.GUITAR, 31);
        fillRange(table, RadioInstrument.BASS, 32, 39);

        set(table, RadioInstrument.FLUTE, 40, 41, 44);
        set(table, RadioInstrument.BASS, 42, 43);
        set(table, RadioInstrument.HARP, 45, 46);
        set(table, RadioInstrument.BASEDRUM, 47);
        fillRange(table, RadioInstrument.FLUTE, 48, 54);
        set(table, RadioInstrument.BIT, 55);

        set(table, RadioInstrument.TRUMPET, 56, 59, 61, 62, 63);
        set(table, RadioInstrument.TRUMPET_WEATHERED, 57);
        set(table, RadioInstrument.TRUMPET_OXIDIZED, 58);
        set(table, RadioInstrument.TRUMPET_EXPOSED, 60);

        fillRange(table, RadioInstrument.FLUTE, 64, 69);
        set(table, RadioInstrument.DIDGERIDOO, 70);
        set(table, RadioInstrument.FLUTE, 71);
        fillRange(table, RadioInstrument.FLUTE, 72, 79);

        fillRange(table, RadioInstrument.BIT, 80, 87);
        fillRange(table, RadioInstrument.PLING, 88, 103);

        fillRange(table, RadioInstrument.BANJO, 104, 106);
        set(table, RadioInstrument.HARP, 107);
        set(table, RadioInstrument.IRON_XYLOPHONE, 108);
        set(table, RadioInstrument.DIDGERIDOO, 109);
        set(table, RadioInstrument.FLUTE, 110, 111);
        set(table, RadioInstrument.BELL, 112);
        set(table, RadioInstrument.COW_BELL, 113);
        set(table, RadioInstrument.IRON_XYLOPHONE, 114);
        set(table, RadioInstrument.HAT, 115);
        fillRange(table, RadioInstrument.BASEDRUM, 116, 118);
        set(table, RadioInstrument.SNARE, 119);
        fillRange(table, RadioInstrument.HAT, 120, 127);
        return table;
    }

    private static DrumSound[] drumTable() {
        DrumSound[] table = new DrumSound[MIDI_VALUES];
        Arrays.fill(table, new DrumSound(RadioInstrument.HAT, 12));
        set(table, new DrumSound(RadioInstrument.BASEDRUM, 6), 35, 36);
        set(table, new DrumSound(RadioInstrument.HAT, 18), 37);
        set(table, new DrumSound(RadioInstrument.SNARE, 12), 38, 40);
        set(table, new DrumSound(RadioInstrument.SNARE, 16), 39);
        set(table, new DrumSound(RadioInstrument.BASEDRUM, 10), 41, 43);
        set(table, new DrumSound(RadioInstrument.HAT, 16), 42);
        set(table, new DrumSound(RadioInstrument.HAT, 12), 44);
        set(table, new DrumSound(RadioInstrument.BASEDRUM, 14), 45, 47);
        set(table, new DrumSound(RadioInstrument.HAT, 10), 46);
        set(table, new DrumSound(RadioInstrument.BASEDRUM, 18), 48, 50);
        set(table, new DrumSound(RadioInstrument.SNARE, 20), 49, 52, 55, 57);
        set(table, new DrumSound(RadioInstrument.HAT, 20), 51, 53, 59);
        set(table, new DrumSound(RadioInstrument.HAT, 22), 54, 69, 70, 82);
        set(table, new DrumSound(RadioInstrument.COW_BELL, 12), 56);
        fillRange(table, new DrumSound(RadioInstrument.BASEDRUM, 16), 60, 66);
        set(table, new DrumSound(RadioInstrument.COW_BELL, 16), 67, 68);
        set(table, new DrumSound(RadioInstrument.HAT, 14), 75, 76, 77);
        set(table, new DrumSound(RadioInstrument.CHIME, 18), 80, 81);
        return table;
    }

    /** Puts {@code value} at each of the given indexes. */
    private static <T> void set(T[] table, T value, int... indexes) {
        for (int index : indexes) {
            table[index] = value;
        }
    }

    /** Puts {@code value} at every index from {@code first} to {@code last}, both included. */
    private static <T> void fillRange(T[] table, T value, int first, int last) {
        Arrays.fill(table, first, last + 1, value);
    }
}
