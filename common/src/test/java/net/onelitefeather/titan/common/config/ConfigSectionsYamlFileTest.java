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
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers {@link ConfigSections} binding a section read through the real {@code avaje-config} +
 * SnakeYAML pipeline from a hand-written {@code application.yaml} file - as opposed to every other
 * {@code ConfigSections*Test} class, which builds its {@link Configuration} directly from a {@link
 * java.util.Map} (design.md decision 1's spike result: no file system involved there). A YAML list
 * of scalars ({@code sit.allowedBlocks}), a nested map ({@code sit.offset}) and a map of records
 * ({@code navigator.entries.<name>}) are exactly the three shapes {@code design.md} decision 2's
 * spike identified as the ones {@code avaje-config} flattens - this test is the one place all three
 * go through SnakeYAML itself instead of a hand-built flat {@link java.util.Map}.
 */
class ConfigSectionsYamlFileTest {

    @Test
    @DisplayName("A list, a nested map and a map of records loaded from a real YAML file all bind correctly")
    void listNestedMapAndMapOfRecordsBindCorrectlyFromARealYamlFile(@TempDir Path tempDir) throws IOException {
        Path applicationYaml = tempDir.resolve("application.yaml");
        Files.writeString(applicationYaml, """
                sit:
                  offset:
                    x: 1.5
                    y: 2.25
                    z: 3.5
                  allowedBlocks:
                    - minecraft:stone
                    - minecraft:dirt
                navigator:
                  title: Navigator
                  entries:
                    survival:
                      slot: 3
                      destination: survival-lobby
                    parkour:
                      slot: 5
                      destination: parkour-lobby
                """);

        Configuration configuration = Configuration.builder().load(applicationYaml.toFile()).build();
        ConfigSections sections = new ConfigSections(configuration);

        SitTestConfig sit = sections.section("sit", SitTestConfig.class, SitTestConfig.DEFAULTS);
        assertEquals(new Vec(1.5, 2.25, 3.5), sit.offset(), "the nested map sit.offset must bind from the real YAML file");
        assertEquals(List.of(Key.key("minecraft:stone"), Key.key("minecraft:dirt")), sit.allowedBlocks(), "the list sit.allowedBlocks must bind from the real YAML file");

        NavigatorTestConfig navigator = sections.section("navigator", NavigatorTestConfig.class, NavigatorTestConfig.DEFAULTS);
        assertEquals("Navigator", navigator.title());
        assertEquals(new NavigatorTestConfig.Entry(3, "survival-lobby"), navigator.entries().get("survival"), "the map-of-records entry navigator.entries.survival must bind from the real YAML file");
        assertEquals(new NavigatorTestConfig.Entry(5, "parkour-lobby"), navigator.entries().get("parkour"), "the map-of-records entry navigator.entries.parkour must bind from the real YAML file");
    }
}
