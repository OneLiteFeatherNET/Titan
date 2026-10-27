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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InstrumentMapperTest {

    private static final int DRUM_CHANNEL = 9;
    private static final int MELODIC_CHANNEL = 0;
    private static final int ANY_KEY = 60;

    @DisplayName("Every General MIDI program group plays on its note block instrument")
    @Test
    void melodicProgramGroupsMapToTheirInstrument() {
        int[] programs = {0, 8, 12, 14, 15, 16, 24, 32, 40, 64, 80, 88, 104, 112, 120};
        RadioInstrument[] instruments = {RadioInstrument.HARP, RadioInstrument.CHIME, RadioInstrument.XYLOPHONE, RadioInstrument.BELL, RadioInstrument.IRON_XYLOPHONE, RadioInstrument.FLUTE, RadioInstrument.GUITAR, RadioInstrument.BASS, RadioInstrument.HARP, RadioInstrument.FLUTE, RadioInstrument.BIT, RadioInstrument.PLING, RadioInstrument.BANJO, RadioInstrument.COW_BELL, RadioInstrument.HAT};

        for (int i = 0; i < programs.length; i++) {
            Assertions.assertEquals(instruments[i], InstrumentMapper.of(MELODIC_CHANNEL, programs[i], ANY_KEY), "program " + programs[i] + " must play " + instruments[i]);
        }
    }

    @DisplayName("Brass programs 56, 59 and 61 to 63 play the trumpet, the trombone, tuba and french horn have their own variants")
    @Test
    void brassProgramsMapToTheTrumpetFamily() {
        for (int program : new int[]{56, 59, 61, 62, 63}) {
            Assertions.assertEquals(RadioInstrument.TRUMPET, InstrumentMapper.of(MELODIC_CHANNEL, program, ANY_KEY), "program " + program + " must play the trumpet");
        }
        Assertions.assertEquals(RadioInstrument.TRUMPET_WEATHERED, InstrumentMapper.of(MELODIC_CHANNEL, 57, ANY_KEY), "the trombone, program 57, must play the weathered trumpet");
        Assertions.assertEquals(RadioInstrument.TRUMPET_OXIDIZED, InstrumentMapper.of(MELODIC_CHANNEL, 58, ANY_KEY), "the tuba, program 58, must play the oxidized trumpet");
        Assertions.assertEquals(RadioInstrument.TRUMPET_EXPOSED, InstrumentMapper.of(MELODIC_CHANNEL, 60, ANY_KEY), "the french horn, program 60, must play the exposed trumpet");
    }

    @DisplayName("Channel ten is drums: the key picks the drum, the program is ignored")
    @Test
    void drumChannelPicksTheInstrumentByKey() {
        Assertions.assertEquals(RadioInstrument.BASEDRUM, InstrumentMapper.of(DRUM_CHANNEL, 0, 36), "the bass drum key must play basedrum");
        Assertions.assertEquals(RadioInstrument.SNARE, InstrumentMapper.of(DRUM_CHANNEL, 0, 38), "the snare key must play snare");
        Assertions.assertEquals(RadioInstrument.HAT, InstrumentMapper.of(DRUM_CHANNEL, 0, 42), "the closed hi-hat key must play hat");
        Assertions.assertEquals(RadioInstrument.COW_BELL, InstrumentMapper.of(DRUM_CHANNEL, 0, 56), "the cowbell key must play cow_bell");
        Assertions.assertEquals(RadioInstrument.HAT, InstrumentMapper.of(DRUM_CHANNEL, 0, 60), "any other drum key falls back to hat");
    }
}
