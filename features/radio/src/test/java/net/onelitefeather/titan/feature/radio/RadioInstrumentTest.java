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

class RadioInstrumentTest {

    @DisplayName("Each instrument's base key is the MIDI key of its note 0, following the vanilla registers")
    @Test
    void baseKeysFollowTheVanillaRegisters() {
        Assertions.assertEquals(54, RadioInstrument.HARP.baseKey(), "the harp must start at F#3");
        Assertions.assertEquals(30, RadioInstrument.BASS.baseKey(), "the bass must start at F#1");
        Assertions.assertEquals(30, RadioInstrument.DIDGERIDOO.baseKey(), "the didgeridoo must start at F#1");
        Assertions.assertEquals(42, RadioInstrument.GUITAR.baseKey(), "the guitar must start at F#2");
        Assertions.assertEquals(66, RadioInstrument.FLUTE.baseKey(), "the flute must start at F#4");
        Assertions.assertEquals(78, RadioInstrument.BELL.baseKey(), "the bell must start at F#5");
        Assertions.assertEquals(66, RadioInstrument.COW_BELL.baseKey(), "the cow bell must start at F#4");
        Assertions.assertEquals(78, RadioInstrument.CHIME.baseKey(), "the chime must start at F#5");
        Assertions.assertEquals(78, RadioInstrument.XYLOPHONE.baseKey(), "the xylophone must start at F#5");
    }

    @DisplayName("The trumpet family shares the harp register until it is confirmed by ear in game")
    @Test
    void trumpetsUseTheHarpRegister() {
        for (RadioInstrument trumpet : List.of(RadioInstrument.TRUMPET, RadioInstrument.TRUMPET_EXPOSED, RadioInstrument.TRUMPET_WEATHERED, RadioInstrument.TRUMPET_OXIDIZED)) {
            Assertions.assertEquals(54, trumpet.baseKey(), trumpet + " must start at key 54 until the register is confirmed");
        }
    }

    @DisplayName("An instrument reaches 25 notes from its base key and no further")
    @Test
    void registerIsTwentyFiveNotesFromTheBaseKey() {
        Assertions.assertTrue(RadioInstrument.BASS.reaches(30), "the bass must reach its base key 30");
        Assertions.assertTrue(RadioInstrument.BASS.reaches(54), "the bass must reach its top note 54");
        Assertions.assertFalse(RadioInstrument.BASS.reaches(29), "the bass must not reach key 29");
        Assertions.assertFalse(RadioInstrument.BASS.reaches(55), "the bass must not reach key 55");
    }

    @DisplayName("The semitone of a key is the key minus the base key of the instrument")
    @Test
    void semitoneIsTheKeyAboveTheBaseKey() {
        Assertions.assertEquals(13, RadioInstrument.FLUTE.semitoneOf(79), "key 79 on the flute must be note 13");
    }

    @DisplayName("A note inside its instrument's register stays on that instrument")
    @Test
    void noteInsideItsRegisterStaysOnItsInstrument() {
        Assertions.assertEquals(RadioInstrument.HARP, RadioInstrument.HARP.playing(66), "key 66 must stay on the harp");
    }

    @DisplayName("A harp note just above F#5 moves to the flute, the register nearest to the harp")
    @Test
    void noteAboveTheHarpMovesToTheFlute() {
        Assertions.assertEquals(RadioInstrument.FLUTE, RadioInstrument.HARP.playing(79), "key 79 must play on the flute");
    }

    @DisplayName("A harp note above the flute register moves to the bell")
    @Test
    void noteAboveTheFluteMovesToTheBell() {
        Assertions.assertEquals(RadioInstrument.BELL, RadioInstrument.HARP.playing(96), "key 96 must play on the bell");
    }

    @DisplayName("A harp note below the harp register prefers the guitar over the bass, the register nearest to the harp")
    @Test
    void noteBelowTheHarpPrefersTheNearestRegister() {
        Assertions.assertEquals(RadioInstrument.GUITAR, RadioInstrument.HARP.playing(50), "key 50 is inside the bass and the guitar, the guitar is nearer and must win");
    }

    @DisplayName("A harp note only the bass reaches moves to the bass")
    @Test
    void noteOnlyTheBassReachesMovesToTheBass() {
        Assertions.assertEquals(RadioInstrument.BASS, RadioInstrument.HARP.playing(35), "key 35 must play on the bass");
    }

    @DisplayName("A bell note below the bell register moves to the guitar")
    @Test
    void noteBelowTheBellMovesToTheGuitar() {
        Assertions.assertEquals(RadioInstrument.GUITAR, RadioInstrument.BELL.playing(60), "key 60 must play on the guitar");
    }

    @DisplayName("Every key from F#1 to F#7 is played by some instrument")
    @Test
    void everyKeyOfTheSpanIsPlayed() {
        for (int key = Pitch.SPAN_LOWEST_KEY; key <= Pitch.SPAN_HIGHEST_KEY; key++) {
            Assertions.assertTrue(RadioInstrument.HARP.playing(key).reaches(key), "key " + key + " must be played by an instrument whose register holds it");
        }
    }

    @DisplayName("A key outside every register has no instrument to play it")
    @Test
    void keyOutsideEveryRegisterIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> RadioInstrument.HARP.playing(20), "key 20 must not be played without folding first");
    }
}
