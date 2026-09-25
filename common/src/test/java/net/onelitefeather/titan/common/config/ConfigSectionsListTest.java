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
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers {@link ConfigSections} splitting a single, comma-joined value back into a list - the
 * representation {@code avaje-config} itself gives a YAML list of scalars, confirmed empirically
 * in {@code design.md} decision 2 (e.g. {@code sit.allowedBlocks=stone,dirt,special,grass}). Only
 * a record component actually typed {@code List}/{@code Set}/array is split this way; every other
 * value is left as a single value for Gson to coerce.
 */
class ConfigSectionsListTest {

    @Test
    @DisplayName("A comma-joined value for a List component is split into its elements")
    void listOfStringsIsSplitOnComma() {
        Configuration configuration = Configuration.builder().putAll(Map.of(
                "sit.allowedBlocks", "minecraft:stone,minecraft:dirt,minecraft:grass_block"
        )).build();
        ConfigSections sections = new ConfigSections(configuration);

        SitTestConfig sit = sections.section("sit", SitTestConfig.class, SitTestConfig.DEFAULTS);

        assertEquals(List.of(Key.key("minecraft:stone"), Key.key("minecraft:dirt"), Key.key("minecraft:grass_block")), sit.allowedBlocks());
    }

    @Test
    @DisplayName("A single-element list is not mistaken for a scalar")
    void singleElementListStaysAList() {
        Configuration configuration = Configuration.builder().putAll(Map.of("sit.allowedBlocks", "minecraft:stone")).build();
        ConfigSections sections = new ConfigSections(configuration);

        SitTestConfig sit = sections.section("sit", SitTestConfig.class, SitTestConfig.DEFAULTS);

        assertEquals(List.of(Key.key("minecraft:stone")), sit.allowedBlocks());
    }

    @Test
    @DisplayName("An empty override for a List component yields an empty list, not a list with one empty element")
    void emptyOverrideYieldsAnEmptyList() {
        Configuration configuration = Configuration.builder().putAll(Map.of("sit.allowedBlocks", "")).build();
        ConfigSections sections = new ConfigSections(configuration);

        SitTestConfig sit = sections.section("sit", SitTestConfig.class, SitTestConfig.DEFAULTS);

        assertEquals(List.of(), sit.allowedBlocks(), "an empty raw value must bind to an empty list, not [\"\"]");
    }
}
