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
package net.onelitefeather.titan.platform.otis;

import java.util.Map;
import net.onelitefeather.titan.core.settings.SettingKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OtisJsonTest {

    private static final SettingKey TAG = SettingKey.parse("titan:tag.enabled");

    @DisplayName("A stored setting is read with its key and boolean value")
    @Test
    void storedSettingIsRead() {
        Map<SettingKey, Boolean> values = OtisJson.parseSettings("[{\"key\":\"titan:tag.enabled\",\"value\":true,\"version\":1,\"updatedAt\":\"2026-10-24T12:00:00Z\"}]");
        Assertions.assertEquals(Map.of(TAG, true), values, "the entry must map its key to its value");
    }

    @DisplayName("Fields the adapter does not read are ignored")
    @Test
    void unknownFieldsAreIgnored() {
        Map<SettingKey, Boolean> values = OtisJson.parseSettings("[{\"key\":\"titan:tag.enabled\",\"value\":false,\"extra\":{\"nested\":[1,2]}}]");
        Assertions.assertEquals(Map.of(TAG, false), values, "a field the adapter does not use must not break the read");
    }

    @DisplayName("An entry with a value that is not a boolean is skipped")
    @Test
    void nonBooleanValueIsSkipped() {
        Map<SettingKey, Boolean> values = OtisJson.parseSettings("[{\"key\":\"titan:tag.enabled\",\"value\":\"yes\"}]");
        Assertions.assertTrue(values.isEmpty(), "a value that is not a boolean must not be read as one");
    }

    @DisplayName("An entry with a key that breaks Otis' rules is skipped")
    @Test
    void invalidKeyIsSkipped() {
        Map<SettingKey, Boolean> values = OtisJson.parseSettings("[{\"key\":\"titan:Tag\",\"value\":true}]");
        Assertions.assertTrue(values.isEmpty(), "a key the registry could never have stored must be dropped");
    }

    @DisplayName("A value is written as a JSON object with a value field")
    @Test
    void valueBodyIsAnObject() {
        Assertions.assertEquals("{\"value\":true}", OtisJson.valueBody(true), "the write body must be the value object Otis expects");
    }
}
