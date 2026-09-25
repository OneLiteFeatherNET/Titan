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

import io.avaje.config.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the {@code lobby-module-config} spec scenarios "missing section" and "missing single
 * value" against {@link ConfigSections#section(String, Class, Record)}, backed by a {@link
 * Configuration} built directly from a {@link Map} (no file system involved, per {@code
 * design.md} decision 1's spike result).
 */
class ConfigSectionsDefaultsTest {

    @Test
    @DisplayName("A section missing from the configuration falls back to the module's defaults")
    void missingSectionUsesDefaults() {
        Configuration configuration = Configuration.builder().putAll(Map.of("spawn.minHeight", "-64")).build();
        ConfigSections sections = new ConfigSections(configuration);

        TickleTestConfig tickle = sections.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS);

        assertEquals(TickleTestConfig.DEFAULTS.cooldownMillis(), tickle.cooldownMillis());
    }

    @Test
    @DisplayName("A field missing from a present section falls back to its default, not 0")
    void missingSingleFieldUsesDefaultNotZero() {
        // Only minHeight is set; maxHeight and simulationDistance are absent.
        Configuration configuration = Configuration.builder().putAll(Map.of("spawn.minHeight", "-32")).build();
        ConfigSections sections = new ConfigSections(configuration);

        SpawnTestConfig spawn = sections.section("spawn", SpawnTestConfig.class, SpawnTestConfig.DEFAULTS);

        assertEquals(-32, spawn.minHeight(), "the present field must be read from the configuration");
        assertEquals(SpawnTestConfig.DEFAULTS.maxHeight(), spawn.maxHeight(), "missing field must default, not 0");
        assertEquals(SpawnTestConfig.DEFAULTS.simulationDistance(), spawn.simulationDistance(), "missing primitive field must default, not 0");
    }

    @Test
    @DisplayName("An empty configuration falls back to defaults for every section")
    void emptyConfigurationUsesDefaultsForEverySection() {
        Configuration configuration = Configuration.builder().build();
        ConfigSections sections = new ConfigSections(configuration);

        SpawnTestConfig spawn = sections.section("spawn", SpawnTestConfig.class, SpawnTestConfig.DEFAULTS);

        assertEquals(SpawnTestConfig.DEFAULTS, spawn);
    }
}
