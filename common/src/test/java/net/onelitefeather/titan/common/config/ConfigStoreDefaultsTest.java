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
 * Covers the {@code lobby-module-config} spec scenario "first start" against {@link
 * ConfigStore#flush()} - writing every requested section's defaults to disk on the first start.
 * The binding-core scenarios "missing section" and "missing single value" moved to {@link
 * SectionBinderDefaultsTest}.
 */
class ConfigStoreDefaultsTest {

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
