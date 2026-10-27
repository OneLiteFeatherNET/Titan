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
 * Chooses the note block instrument of a MIDI note from its channel, program and key. Channel ten
 * (index 9) is drums and picks by key; every other channel follows the General MIDI program group.
 */
final class InstrumentMapper {

    static final int DRUM_CHANNEL = 9;

    /**
     * The General MIDI programs 0 to 127 in groups, each ending at the upper bound of its entry.
     */
    private static final int[] MELODIC_UPPER_BOUNDS = {7, 11, 13, 14, 15, 23, 31, 39, 55, 63, 79, 87, 103, 111, 119, 127};
    private static final RadioInstrument[] MELODIC_INSTRUMENTS = {RadioInstrument.HARP, RadioInstrument.CHIME, RadioInstrument.XYLOPHONE, RadioInstrument.BELL, RadioInstrument.IRON_XYLOPHONE, RadioInstrument.FLUTE, RadioInstrument.GUITAR, RadioInstrument.BASS, RadioInstrument.HARP, RadioInstrument.DIDGERIDOO, RadioInstrument.FLUTE, RadioInstrument.BIT, RadioInstrument.PLING, RadioInstrument.BANJO, RadioInstrument.COW_BELL, RadioInstrument.HAT};

    private InstrumentMapper() {
    }

    static RadioInstrument of(int channel, int program, int key) {
        if (channel < 0 || channel > 15 || program < 0 || program > 127 || key < 0 || key > 127) {
            throw new IllegalArgumentException("channel, program and key must be MIDI data values, was " + channel + ", " + program + ", " + key);
        }
        return channel == DRUM_CHANNEL ? drum(key) : melodic(program);
    }

    private static RadioInstrument drum(int key) {
        return switch (key) {
            case 35, 36 -> RadioInstrument.BASEDRUM;
            case 37, 38, 39, 40 -> RadioInstrument.SNARE;
            case 56 -> RadioInstrument.COW_BELL;
            default -> RadioInstrument.HAT;
        };
    }

    private static RadioInstrument melodic(int program) {
        for (int group = 0; group < MELODIC_UPPER_BOUNDS.length; group++) {
            if (program <= MELODIC_UPPER_BOUNDS[group]) {
                return MELODIC_INSTRUMENTS[group];
            }
        }
        throw new IllegalArgumentException("program must be at most 127, was " + program);
    }
}
