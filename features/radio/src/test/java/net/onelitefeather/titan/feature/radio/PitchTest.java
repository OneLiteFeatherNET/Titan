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

    @DisplayName("A key outside the span of all registers is folded by octaves into it and keeps its pitch class")
    @Test
    void foldingIntoTheSpanKeepsThePitchClass() {
        Assertions.assertEquals(32, Pitch.foldIntoSpan(20), "key 20 must fold up to 32, the lowest bass note above the span");
        Assertions.assertEquals(98, Pitch.foldIntoSpan(110), "key 110 must fold down to 98, inside the bell register");
    }

    @DisplayName("A key inside the span of all registers is not moved by folding")
    @Test
    void foldingLeavesKeysInsideTheSpan() {
        Assertions.assertEquals(30, Pitch.foldIntoSpan(30), "the bottom of the span must not move");
        Assertions.assertEquals(66, Pitch.foldIntoSpan(66), "a key inside the span must not move");
        Assertions.assertEquals(102, Pitch.foldIntoSpan(102), "the top of the span must not move");
    }
}
