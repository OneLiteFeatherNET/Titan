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

class PitchTest {

    private static final float DELTA = 1.0e-6f;

    @DisplayName("Note 0 plays at half pitch, note 12 at normal pitch and note 24 at double pitch")
    @Test
    void pitchOfTheNoteBlockRange() {
        Assertions.assertEquals(0.5f, Pitch.of(0), DELTA, "note 0 must play at pitch 0.5");
        Assertions.assertEquals(1.0f, Pitch.of(12), DELTA, "note 12 must play at pitch 1.0");
        Assertions.assertEquals(2.0f, Pitch.of(24), DELTA, "note 24 must play at pitch 2.0");
    }

    @DisplayName("A song is moved in whole octaves so its median note lands on the centre of the range")
    @Test
    void transpositionMovesTheMedianToTheCentre() {
        Assertions.assertEquals(12, Pitch.transposition(0), "a median at the bottom must move up one octave");
        Assertions.assertEquals(-12, Pitch.transposition(24), "a median at the top must move down one octave");
        Assertions.assertEquals(0, Pitch.transposition(12), "a median in the centre must not move");
        Assertions.assertEquals(-12, Pitch.transposition(19), "a median seven semitones above the centre must move down one octave");
    }

    @DisplayName("A transposition exactly halfway between two octaves rounds down")
    @Test
    void halfwayTranspositionRoundsDown() {
        Assertions.assertEquals(0, Pitch.transposition(6), "a median six semitones below the centre must stay, rounding down");
        Assertions.assertEquals(-12, Pitch.transposition(18), "a median six semitones above the centre must move down, rounding down");
    }

    @DisplayName("A note outside the range is folded by octaves and keeps its pitch class")
    @Test
    void foldingKeepsThePitchClass() {
        Assertions.assertEquals(11, Pitch.fold(-1), "-1 must fold up to 11");
        Assertions.assertEquals(11, Pitch.fold(-13), "-13 must fold up to 11");
        Assertions.assertEquals(13, Pitch.fold(25), "25 must fold down to 13");
        Assertions.assertEquals(18, Pitch.fold(30), "30 must fold down to 18, the first octave inside the range");
        Assertions.assertEquals(12, Pitch.fold(12), "a note inside the range must not move");
        Assertions.assertEquals(24, Pitch.fold(24), "the top of the range must not move");
    }
}
