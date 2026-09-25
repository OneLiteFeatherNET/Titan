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

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@code lobby-module-config} spec scenarios "missing section", "missing single value"
 * and "first start", against {@link ConfigStore#section(String, Class, Record)} and {@link
 * ConfigStore#flush()} directly (no {@code ModuleContext} binding involved; that is task 3.2).
 */
class ConfigStoreDefaultsTest {

    @Test
    @DisplayName("A section missing from the file falls back to the module's defaults")
    void missingSectionUsesDefaults(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        Files.writeString(file, """
                {
                  "configVersion": 2,
                  "spawn": {"minHeight": -64, "maxHeight": 310, "simulationDistance": 2}
                }
                """);

        ConfigStore store = ConfigStore.open(file);
        TickleTestConfig tickle = store.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS);

        assertEquals(TickleTestConfig.DEFAULTS.cooldownMillis(), tickle.cooldownMillis());
    }

    @Test
    @DisplayName("A field missing from a present section falls back to its default, not 0")
    void missingSingleFieldUsesDefaultNotZero(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        // Only minHeight is set; maxHeight and simulationDistance are absent.
        Files.writeString(file, """
                {
                  "configVersion": 2,
                  "spawn": {"minHeight": -32}
                }
                """);

        ConfigStore store = ConfigStore.open(file);
        SpawnTestConfig spawn = store.section("spawn", SpawnTestConfig.class, SpawnTestConfig.DEFAULTS);

        assertEquals(-32, spawn.minHeight(), "the present field must be read from the file");
        assertEquals(SpawnTestConfig.DEFAULTS.maxHeight(), spawn.maxHeight(), "missing field must default, not 0");
        assertEquals(SpawnTestConfig.DEFAULTS.simulationDistance(), spawn.simulationDistance(), "missing primitive field must default, not 0");
    }

    @Test
    @DisplayName("A missing file is created with every requested section once flush() runs")
    void firstStartCreatesFileWithAllRequestedSectionsAfterFlush(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        assertTrue(Files.notExists(file));

        ConfigStore store = ConfigStore.open(file);
        store.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS);
        store.section("spawn", SpawnTestConfig.class, SpawnTestConfig.DEFAULTS);
        store.flush();

        assertTrue(Files.exists(file), "flush() must create the file on first start");
        JsonObject document = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertEquals(2, document.get("configVersion").getAsInt());
        assertTrue(document.has("tickle"));
        assertTrue(document.has("spawn"));
        assertEquals(TickleTestConfig.DEFAULTS.cooldownMillis(), document.getAsJsonObject("tickle").get("cooldownMillis").getAsLong());
        assertEquals(SpawnTestConfig.DEFAULTS.maxHeight(), document.getAsJsonObject("spawn").get("maxHeight").getAsInt());
    }
}
