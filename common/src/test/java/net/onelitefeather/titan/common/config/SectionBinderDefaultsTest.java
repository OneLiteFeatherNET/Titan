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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the {@code lobby-module-config} spec scenarios "missing section" and "missing single
 * value" against {@link SectionBinder#bind(String, String, Class, Record, JsonElement)} directly,
 * without a {@link ConfigStore} document or a {@link ConfigSections} configuration around it.
 */
class SectionBinderDefaultsTest {

    private final SectionBinder binder = new SectionBinder();

    @Test
    @DisplayName("A missing section falls back to the module's defaults")
    void missingSectionUsesDefaults() {
        TickleTestConfig tickle = binder.bind("app.json", "tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS, null).value();

        assertEquals(TickleTestConfig.DEFAULTS.cooldownMillis(), tickle.cooldownMillis());
    }

    @Test
    @DisplayName("A field missing from a present section falls back to its default, not 0")
    void missingSingleFieldUsesDefaultNotZero() {
        // Only minHeight is set; maxHeight and simulationDistance are absent.
        JsonElement existing = JsonParser.parseString("{\"minHeight\": -32}");

        SpawnTestConfig spawn = binder.bind("app.json", "spawn", SpawnTestConfig.class, SpawnTestConfig.DEFAULTS, existing).value();

        assertEquals(-32, spawn.minHeight(), "the present field must be read from the section");
        assertEquals(SpawnTestConfig.DEFAULTS.maxHeight(), spawn.maxHeight(), "missing field must default, not 0");
        assertEquals(SpawnTestConfig.DEFAULTS.simulationDistance(), spawn.simulationDistance(), "missing primitive field must default, not 0");
    }
}
