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
 * Covers {@link ConfigSections} rebuilding a {@code Map<String, Record>} section from dotted keys
 * named after the map key, e.g. {@code navigator.entries.survival.slot} - the shape {@code
 * design.md} decision 3 relies on to let a single navigator entry be overridden by name.
 */
class ConfigSectionsMapOfRecordsTest {

    @Test
    @DisplayName("A new map entry is added alongside the default entries")
    void newMapEntryIsAddedAlongsideDefaults() {
        Configuration configuration = Configuration.builder().putAll(Map.of(
                "navigator.entries.parkour.slot", "2", "navigator.entries.parkour.destination", "parkour-lobby"
        )).build();
        ConfigSections sections = new ConfigSections(configuration);

        NavigatorTestConfig navigator = sections.section("navigator", NavigatorTestConfig.class, NavigatorTestConfig.DEFAULTS);

        assertEquals(new NavigatorTestConfig.Entry(2, "parkour-lobby"), navigator.entries().get("parkour"), "the new entry must be present");
        assertEquals(NavigatorTestConfig.DEFAULTS.entries().get("survival"), navigator.entries().get("survival"), "the default entry must still be present");
    }

    @Test
    @DisplayName("A single field of one map entry can be overridden, keeping its other fields")
    void singleFieldOfOneMapEntryIsOverridden() {
        Configuration configuration = Configuration.builder().putAll(Map.of("navigator.entries.survival.slot", "9")).build();
        ConfigSections sections = new ConfigSections(configuration);

        NavigatorTestConfig navigator = sections.section("navigator", NavigatorTestConfig.class, NavigatorTestConfig.DEFAULTS);

        NavigatorTestConfig.Entry survival = navigator.entries().get("survival");
        assertEquals(9, survival.slot(), "the overridden field must be read from the configuration");
        assertEquals(NavigatorTestConfig.DEFAULTS.entries().get("survival").destination(), survival.destination(), "the untouched field must default");
    }
}
