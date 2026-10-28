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

    private static void assertPlays(RadioInstrument instrument, int... programs) {
        for (int program : programs) {
            Assertions.assertEquals(instrument, InstrumentMapper.of(MELODIC_CHANNEL, program, ANY_KEY), "program " + program + " must play " + instrument);
        }
    }

    private static void assertDrum(int key, RadioInstrument instrument, int semitone) {
        InstrumentMapper.DrumSound sound = InstrumentMapper.drum(key);
        Assertions.assertEquals(instrument, sound.instrument(), "drum key " + key + " must play " + instrument);
        Assertions.assertEquals(semitone, sound.semitone(), "drum key " + key + " must play note " + semitone);
    }

    @DisplayName("Piano and electric piano programs play the harp or the pling")
    @Test
    void pianosMapToHarpAndPling() {
        assertPlays(RadioInstrument.HARP, 0, 1, 2, 3);
        assertPlays(RadioInstrument.PLING, 4, 5);
        assertPlays(RadioInstrument.GUITAR, 6);
        assertPlays(RadioInstrument.BANJO, 7);
    }

    @DisplayName("Tuned percussion programs play the bell, chime, xylophone or banjo")
    @Test
    void tunedPercussionMapsToBellsAndXylophones() {
        assertPlays(RadioInstrument.BELL, 8, 9);
        assertPlays(RadioInstrument.CHIME, 10, 14);
        assertPlays(RadioInstrument.IRON_XYLOPHONE, 11);
        assertPlays(RadioInstrument.XYLOPHONE, 12, 13);
        assertPlays(RadioInstrument.BANJO, 15);
    }

    @DisplayName("Organs, accordions and harmonicas play the flute")
    @Test
    void organsMapToFlute() {
        assertPlays(RadioInstrument.FLUTE, 16, 17, 18, 19, 20, 21, 22, 23);
    }

    @DisplayName("Guitars play the guitar, distorted guitars play the bit")
    @Test
    void guitarsMapToGuitarAndBit() {
        assertPlays(RadioInstrument.GUITAR, 24, 25, 26, 27, 28, 31);
        assertPlays(RadioInstrument.BIT, 29, 30);
    }

    @DisplayName("Bass programs, including the synth basses, play the bass")
    @Test
    void basesMapToBass() {
        assertPlays(RadioInstrument.BASS, 32, 33, 34, 35, 36, 37, 38, 39);
    }

    @DisplayName("Violin, viola and string ensembles play the flute, cello and contrabass play the bass")
    @Test
    void stringsMapToFluteAndBass() {
        assertPlays(RadioInstrument.FLUTE, 40, 41, 44, 48, 49, 50, 51, 52, 53, 54);
        assertPlays(RadioInstrument.BASS, 42, 43);
    }

    @DisplayName("Pizzicato strings and the orchestral harp play the harp, timpani plays the pitched basedrum, an orchestra hit plays the bit")
    @Test
    void pizzicatoHarpAndTimpaniMap() {
        assertPlays(RadioInstrument.HARP, 45, 46);
        assertPlays(RadioInstrument.BASEDRUM, 47);
        assertPlays(RadioInstrument.BIT, 55);
    }

    @DisplayName("Brass programs 56, 59 and 61 to 63 play the trumpet, the trombone, tuba and french horn have their own variants")
    @Test
    void brassProgramsMapToTheTrumpetFamily() {
        assertPlays(RadioInstrument.TRUMPET, 56, 59, 61, 62, 63);
        Assertions.assertEquals(RadioInstrument.TRUMPET_WEATHERED, InstrumentMapper.of(MELODIC_CHANNEL, 57, ANY_KEY), "the trombone, program 57, must play the weathered trumpet");
        Assertions.assertEquals(RadioInstrument.TRUMPET_OXIDIZED, InstrumentMapper.of(MELODIC_CHANNEL, 58, ANY_KEY), "the tuba, program 58, must play the oxidized trumpet");
        Assertions.assertEquals(RadioInstrument.TRUMPET_EXPOSED, InstrumentMapper.of(MELODIC_CHANNEL, 60, ANY_KEY), "the french horn, program 60, must play the exposed trumpet");
    }

    @DisplayName("Reeds play the flute, bassoon and bagpipe play the didgeridoo")
    @Test
    void reedsMapToFluteAndDidgeridoo() {
        assertPlays(RadioInstrument.FLUTE, 64, 65, 66, 67, 68, 69, 71);
        assertPlays(RadioInstrument.DIDGERIDOO, 70, 109);
    }

    @DisplayName("Pipes play the flute")
    @Test
    void pipesMapToFlute() {
        assertPlays(RadioInstrument.FLUTE, 72, 73, 74, 75, 76, 77, 78, 79);
    }

    @DisplayName("Synth leads play the bit, synth pads and synth effects play the pling")
    @Test
    void synthsMapToBitAndPling() {
        assertPlays(RadioInstrument.BIT, 80, 81, 82, 83, 84, 85, 86, 87);
        assertPlays(RadioInstrument.PLING, 88, 95, 96, 103);
    }

    @DisplayName("Ethnic plucked and wind programs play their closest note block sound")
    @Test
    void ethnicProgramsMap() {
        assertPlays(RadioInstrument.BANJO, 104, 105, 106);
        assertPlays(RadioInstrument.HARP, 107);
        assertPlays(RadioInstrument.IRON_XYLOPHONE, 108, 114);
        assertPlays(RadioInstrument.FLUTE, 110, 111);
    }

    @DisplayName("Percussive and effect programs play the bell, cowbell, hat, basedrum or snare")
    @Test
    void percussiveAndEffectProgramsMap() {
        assertPlays(RadioInstrument.BELL, 112);
        assertPlays(RadioInstrument.COW_BELL, 113);
        assertPlays(RadioInstrument.HAT, 115, 120, 127);
        assertPlays(RadioInstrument.BASEDRUM, 116, 117, 118);
        assertPlays(RadioInstrument.SNARE, 119);
    }

    @DisplayName("Every General MIDI program from 0 to 127 plays an instrument")
    @Test
    void everyProgramPlaysAnInstrument() {
        for (int program = 0; program <= 127; program++) {
            Assertions.assertNotNull(InstrumentMapper.of(MELODIC_CHANNEL, program, ANY_KEY), "program " + program + " must play an instrument");
        }
    }

    @DisplayName("Channel ten is drums: the key picks the drum, the program is ignored")
    @Test
    void drumChannelPicksTheInstrumentByKey() {
        Assertions.assertEquals(RadioInstrument.BASEDRUM, InstrumentMapper.of(DRUM_CHANNEL, 0, 36), "the bass drum key must play basedrum");
        Assertions.assertEquals(RadioInstrument.SNARE, InstrumentMapper.of(DRUM_CHANNEL, 0, 38), "the snare key must play snare");
        Assertions.assertEquals(RadioInstrument.HAT, InstrumentMapper.of(DRUM_CHANNEL, 0, 42), "the closed hi-hat key must play hat");
        Assertions.assertEquals(RadioInstrument.COW_BELL, InstrumentMapper.of(DRUM_CHANNEL, 0, 56), "the cowbell key must play cow_bell");
        Assertions.assertEquals(RadioInstrument.HAT, InstrumentMapper.of(DRUM_CHANNEL, 0, 84), "an unmapped drum key falls back to hat");
    }

    @DisplayName("Kick keys play the basedrum on note 6")
    @Test
    void kickKeysPlayTheLowBasedrum() {
        assertDrum(35, RadioInstrument.BASEDRUM, 6);
        assertDrum(36, RadioInstrument.BASEDRUM, 6);
    }

    @DisplayName("Snare keys play the snare, the hand clap and the side stick play their own notes")
    @Test
    void snareKeysHaveTheirOwnNotes() {
        assertDrum(37, RadioInstrument.HAT, 18);
        assertDrum(38, RadioInstrument.SNARE, 12);
        assertDrum(39, RadioInstrument.SNARE, 16);
        assertDrum(40, RadioInstrument.SNARE, 12);
    }

    @DisplayName("Toms play the basedrum on notes 10, 14 and 18 by pitch")
    @Test
    void tomsPlayTheBasedrumByPitch() {
        assertDrum(41, RadioInstrument.BASEDRUM, 10);
        assertDrum(43, RadioInstrument.BASEDRUM, 10);
        assertDrum(45, RadioInstrument.BASEDRUM, 14);
        assertDrum(47, RadioInstrument.BASEDRUM, 14);
        assertDrum(48, RadioInstrument.BASEDRUM, 18);
        assertDrum(50, RadioInstrument.BASEDRUM, 18);
    }

    @DisplayName("Hi-hats play the hat on notes 16, 12 and 10 by openness")
    @Test
    void hiHatsPlayTheHatByOpenness() {
        assertDrum(42, RadioInstrument.HAT, 16);
        assertDrum(44, RadioInstrument.HAT, 12);
        assertDrum(46, RadioInstrument.HAT, 10);
    }

    @DisplayName("Crashes and splashes play the snare on note 20, rides play the hat on note 20")
    @Test
    void cymbalsHaveTheirOwnNotes() {
        assertDrum(49, RadioInstrument.SNARE, 20);
        assertDrum(52, RadioInstrument.SNARE, 20);
        assertDrum(55, RadioInstrument.SNARE, 20);
        assertDrum(57, RadioInstrument.SNARE, 20);
        assertDrum(51, RadioInstrument.HAT, 20);
        assertDrum(53, RadioInstrument.HAT, 20);
        assertDrum(59, RadioInstrument.HAT, 20);
    }

    @DisplayName("Shakers and tambourines play the hat on note 22, claves and woodblocks on note 14")
    @Test
    void shakersAndWoodblocksHaveTheirOwnNotes() {
        assertDrum(54, RadioInstrument.HAT, 22);
        assertDrum(69, RadioInstrument.HAT, 22);
        assertDrum(70, RadioInstrument.HAT, 22);
        assertDrum(82, RadioInstrument.HAT, 22);
        assertDrum(75, RadioInstrument.HAT, 14);
        assertDrum(76, RadioInstrument.HAT, 14);
        assertDrum(77, RadioInstrument.HAT, 14);
    }

    @DisplayName("Cowbell and agogo play the cow_bell, triangles play the chime")
    @Test
    void bellsAndTrianglesHaveTheirOwnNotes() {
        assertDrum(56, RadioInstrument.COW_BELL, 12);
        assertDrum(67, RadioInstrument.COW_BELL, 16);
        assertDrum(68, RadioInstrument.COW_BELL, 16);
        assertDrum(80, RadioInstrument.CHIME, 18);
        assertDrum(81, RadioInstrument.CHIME, 18);
    }

    @DisplayName("Latin percussion keys 60 to 66 play the basedrum on note 16")
    @Test
    void latinPercussionPlaysTheBasedrumOnNoteSixteen() {
        for (int key = 60; key <= 66; key++) {
            assertDrum(key, RadioInstrument.BASEDRUM, 16);
        }
    }

    @DisplayName("Any drum key without an entry plays the hat on note 12")
    @Test
    void unmappedDrumKeyPlaysTheHatOnNoteTwelve() {
        assertDrum(84, RadioInstrument.HAT, 12);
    }
}
