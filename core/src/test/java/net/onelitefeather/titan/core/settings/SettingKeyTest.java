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
package net.onelitefeather.titan.core.settings;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SettingKeyTest {

    @DisplayName("A key in Otis' format is accepted and keeps its namespace and value")
    @Test
    void validKeyIsParsed() {
        SettingKey key = SettingKey.parse("titan:tag.enabled");
        Assertions.assertEquals("titan", key.namespace(), "the part before the colon is the namespace");
        Assertions.assertEquals("tag.enabled", key.value(), "the part after the colon is the value");
    }

    @DisplayName("A key prints back in the form Otis uses")
    @Test
    void keyPrintsAsNamespaceColonValue() {
        Assertions.assertEquals("titan:throw.target", SettingKey.parse("titan:throw.target").toString(), "toString must be namespace:value");
    }

    @DisplayName("Upper case letters in the value are rejected")
    @Test
    void upperCaseValueIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SettingKey.parse("titan:Tag"), "Otis allows only lower case in the value");
    }

    @DisplayName("A space in the value is rejected")
    @Test
    void spaceInValueIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SettingKey.parse("titan:tag enabled"), "a space is not an Otis character");
    }

    @DisplayName("A key without a namespace is rejected")
    @Test
    void missingNamespaceIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SettingKey.parse("tag.enabled"), "a key needs namespace:value");
    }

    @DisplayName("An empty value is rejected")
    @Test
    void emptyValueIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SettingKey.parse("titan:"), "the value must not be empty");
    }

    @DisplayName("A second colon is rejected, since colons are not part of the value")
    @ParameterizedTest
    @ValueSource(strings = {"titan:a:b", "Titan:tag.enabled"})
    void extraColonOrUpperCaseNamespaceIsRejected(String raw) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SettingKey.parse(raw), "'" + raw + "' breaks Otis' key rules");
    }
}
