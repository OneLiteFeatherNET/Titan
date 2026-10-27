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
import net.onelitefeather.titan.core.settings.SettingValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OtisJsonTest {

    private static final SettingKey TAG = SettingKey.parse("titan:tag.enabled");
    private static final SettingKey VOLUME = SettingKey.parse("titan:radio.volume");

    @DisplayName("A stored switch is read with its key and boolean value")
    @Test
    void storedSwitchIsRead() {
        Map<SettingKey, SettingValue> values = OtisJson.parseSettings("[{\"key\":\"titan:tag.enabled\",\"value\":true,\"version\":1,\"updatedAt\":\"2026-10-24T12:00:00Z\"}]");

        Assertions.assertEquals(Map.of(TAG, new SettingValue.Flag(true)), values, "the entry must map its key to its flag");
    }

    @DisplayName("A stored whole number is read as a level")
    @Test
    void storedWholeNumberIsReadAsLevel() {
        Map<SettingKey, SettingValue> values = OtisJson.parseSettings("[{\"key\":\"titan:radio.volume\",\"value\":75}]");

        Assertions.assertEquals(Map.of(VOLUME, new SettingValue.Level(75)), values, "a JSON number must be read as a level");
    }

    @DisplayName("Fields the adapter does not read are ignored")
    @Test
    void unknownFieldsAreIgnored() {
        Map<SettingKey, SettingValue> values = OtisJson.parseSettings("[{\"key\":\"titan:tag.enabled\",\"value\":false,\"extra\":{\"nested\":[1,2]}}]");

        Assertions.assertEquals(Map.of(TAG, new SettingValue.Flag(false)), values, "a field the adapter does not use must not break the read");
    }

    @DisplayName("A text value is skipped")
    @Test
    void textValueIsSkipped() {
        Map<SettingKey, SettingValue> values = OtisJson.parseSettings("[{\"key\":\"titan:radio.volume\",\"value\":\"yes\"}]");

        Assertions.assertTrue(values.isEmpty(), "a value that is text must not be read as a flag or a level");
    }

    @DisplayName("A fraction is skipped, since a level is a whole number")
    @Test
    void fractionIsSkipped() {
        Map<SettingKey, SettingValue> values = OtisJson.parseSettings("[{\"key\":\"titan:radio.volume\",\"value\":75.5}]");

        Assertions.assertTrue(values.isEmpty(), "a fraction must not be read as a level");
    }

    @DisplayName("A number too large for a level is skipped")
    @Test
    void hugeNumberIsSkipped() {
        Map<SettingKey, SettingValue> values = OtisJson.parseSettings("[{\"key\":\"titan:radio.volume\",\"value\":99999999999}]");

        Assertions.assertTrue(values.isEmpty(), "a number outside the int range must not be read as a level");
    }

    @DisplayName("An entry with a key that breaks Otis' rules is skipped")
    @Test
    void invalidKeyIsSkipped() {
        Map<SettingKey, SettingValue> values = OtisJson.parseSettings("[{\"key\":\"titan:Tag\",\"value\":true}]");

        Assertions.assertTrue(values.isEmpty(), "a key the registry could never have stored must be dropped");
    }

    @DisplayName("A flag is written as a JSON object with a boolean value field")
    @Test
    void flagBodyIsAnObject() {
        Assertions.assertEquals("{\"value\":true}", OtisJson.valueBody(new SettingValue.Flag(true)), "the write body of a flag must be the value object Otis expects");
    }

    @DisplayName("A level is written as a JSON object with a number value field")
    @Test
    void levelBodyIsAnObjectWithANumber() {
        Assertions.assertEquals("{\"value\":75}", OtisJson.valueBody(new SettingValue.Level(75)), "the write body of a level must carry the number");
    }
}
