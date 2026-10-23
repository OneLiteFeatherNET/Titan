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
package net.onelitefeather.titan.feature.dance;

import java.util.Optional;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DancePayloadTest {

    @DisplayName("A payload naming a known dance yields that dance")
    @Test
    void knownIdYieldsTheDance() {
        Assertions.assertEquals(Optional.of(Dance.GROOVE), DancePayload.dance(DancePayload.of(Dance.GROOVE)), "the payload a button sends must name its dance");
    }

    @DisplayName("A payload with an unknown id yields nothing")
    @Test
    void unknownIdYieldsNothing() {
        CompoundBinaryTag payload = CompoundBinaryTag.builder().putString("id", "moonwalk").build();

        Assertions.assertTrue(DancePayload.dance(payload).isEmpty(), "an unknown id must be ignored");
    }

    @DisplayName("A payload without an id yields nothing")
    @Test
    void missingIdYieldsNothing() {
        Assertions.assertTrue(DancePayload.dance(CompoundBinaryTag.empty()).isEmpty(), "a payload without an id must be ignored");
    }

    @DisplayName("A payload whose id is not a string yields nothing")
    @Test
    void nonStringIdYieldsNothing() {
        CompoundBinaryTag payload = CompoundBinaryTag.builder().put("id", IntBinaryTag.intBinaryTag(1)).build();

        Assertions.assertTrue(DancePayload.dance(payload).isEmpty(), "a numeric id must be ignored");
    }

    @DisplayName("A missing payload or a payload of another kind yields nothing")
    @Test
    void missingOrForeignPayloadYieldsNothing() {
        Assertions.assertTrue(DancePayload.dance(null).isEmpty(), "no payload at all must be ignored");
        Assertions.assertTrue(DancePayload.dance(StringBinaryTag.stringBinaryTag("wave")).isEmpty(), "a bare string must be ignored");
    }
}
