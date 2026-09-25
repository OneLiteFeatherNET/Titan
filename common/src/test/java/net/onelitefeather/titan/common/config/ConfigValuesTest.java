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
package net.onelitefeather.titan.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link ConfigValues#parseInt(String, String)}, {@link ConfigValues#parseLong(String,
 * String)} and {@link ConfigValues#parseDouble(String, String)} - the pure parsing logic behind
 * {@link ConfigValues#intValue(String)}, {@link ConfigValues#longValue(String)} and
 * {@link ConfigValues#doubleValue(String)} (see design.md, decision 4).
 *
 * <p>None of these tests call {@code io.avaje.config.Config} - the parse functions take the raw
 * string directly, so this whole class is free of the static facade's global state (design.md,
 * decision 5).
 */
class ConfigValuesTest {

    @DisplayName("parseInt: a valid whole number is parsed")
    @Test
    void parseIntValidNumber() {
        assertEquals(4000, ConfigValues.parseInt("tickle.cooldownMillis", "4000"));
    }

    @DisplayName("parseInt: a non-numeric value is rejected, naming the full key and the raw value")
    @Test
    void parseIntRejectsNonNumeric() {
        ConfigException thrown = assertThrows(ConfigException.class, () -> ConfigValues.parseInt("tickle.cooldownMillis", "abc"));

        assertEquals("tickle.cooldownMillis", thrown.field());
        assertTrue(thrown.getMessage().contains("tickle.cooldownMillis"), "message must name the full key: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains("'abc'"), "message must quote the raw value: " + thrown.getMessage());
    }

    @DisplayName("parseInt: an empty value is rejected, naming the full key")
    @Test
    void parseIntRejectsEmpty() {
        ConfigException thrown = assertThrows(ConfigException.class, () -> ConfigValues.parseInt("tickle.cooldownMillis", ""));

        assertEquals("tickle.cooldownMillis", thrown.field());
    }

    @DisplayName("parseInt: a value outside the int range is rejected")
    @Test
    void parseIntRejectsOverflow() {
        ConfigException thrown = assertThrows(ConfigException.class, () -> ConfigValues.parseInt("spawn.minHeight", "99999999999"));

        assertEquals("spawn.minHeight", thrown.field());
    }

    @DisplayName("parseLong: a valid whole number is parsed")
    @Test
    void parseLongValidNumber() {
        assertEquals(4000L, ConfigValues.parseLong("tickle.cooldownMillis", "4000"));
    }

    @DisplayName("parseLong: a non-numeric value is rejected, naming the full key and the raw value")
    @Test
    void parseLongRejectsNonNumeric() {
        ConfigException thrown = assertThrows(ConfigException.class, () -> ConfigValues.parseLong("tickle.cooldownMillis", "abc"));

        assertEquals("tickle.cooldownMillis", thrown.field());
        assertTrue(thrown.getMessage().contains("tickle.cooldownMillis"), "message must name the full key: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains("'abc'"), "message must quote the raw value: " + thrown.getMessage());
    }

    @DisplayName("parseLong: an empty value is rejected, naming the full key")
    @Test
    void parseLongRejectsEmpty() {
        ConfigException thrown = assertThrows(ConfigException.class, () -> ConfigValues.parseLong("tickle.cooldownMillis", ""));

        assertEquals("tickle.cooldownMillis", thrown.field());
    }

    @DisplayName("parseLong: a value outside the long range is rejected")
    @Test
    void parseLongRejectsOverflow() {
        ConfigException thrown = assertThrows(ConfigException.class, () -> ConfigValues.parseLong("tickle.cooldownMillis", "99999999999999999999"));

        assertEquals("tickle.cooldownMillis", thrown.field());
    }

    @DisplayName("parseDouble: a valid number is parsed")
    @Test
    void parseDoubleValidNumber() {
        assertEquals(0.5, ConfigValues.parseDouble("sit.offset.x", "0.5"));
    }

    @DisplayName("parseDouble: a non-numeric value is rejected, naming the full key and the raw value")
    @Test
    void parseDoubleRejectsNonNumeric() {
        ConfigException thrown = assertThrows(ConfigException.class, () -> ConfigValues.parseDouble("sit.offset.x", "abc"));

        assertEquals("sit.offset.x", thrown.field());
        assertTrue(thrown.getMessage().contains("sit.offset.x"), "message must name the full key: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains("'abc'"), "message must quote the raw value: " + thrown.getMessage());
    }

    @DisplayName("parseDouble: an empty value is rejected, naming the full key")
    @Test
    void parseDoubleRejectsEmpty() {
        ConfigException thrown = assertThrows(ConfigException.class, () -> ConfigValues.parseDouble("sit.offset.x", ""));

        assertEquals("sit.offset.x", thrown.field());
    }
}
