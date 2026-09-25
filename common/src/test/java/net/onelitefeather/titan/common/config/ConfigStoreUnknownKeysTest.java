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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.onelitefeather.titan.common.config.testing.CapturingLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link ConfigStore}'s own responsibility for a section with an unknown key: unlike
 * {@link LegacyConfigMigration}'s dropped legacy keys, an unknown key is never removed - {@link
 * ConfigStore#save()} writes it back exactly as read. The warning itself (naming the file, the
 * section and the key) moved to {@link SectionBinderUnknownKeysTest}, using the same {@link
 * CapturingLoggerFactory} SLF4J test provider {@link LegacyConfigMigrationTest} uses.
 */
class ConfigStoreUnknownKeysTest {

    @BeforeEach
    void clearLog() {
        CapturingLoggerFactory.clear();
    }

    @Test
    @DisplayName("An unknown key is neither dropped nor rewritten - it survives a save unchanged")
    void unknownKeyIsNeitherDroppedNorRewritten(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        Files.writeString(file, """
                {
                  "configVersion": 2,
                  "elytra": {"boostMultiplier": 1.0, "fireworkBoostSlot": 45}
                }
                """);
        ConfigStore store = ConfigStore.open(file);

        store.section("elytra", ElytraTestConfig.class, ElytraTestConfig.DEFAULTS);
        store.save();

        JsonObject document = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertTrue(document.getAsJsonObject("elytra").has("fireworkBoostSlot"), "an unknown key must survive unchanged, not be dropped");
        assertEquals(45, document.getAsJsonObject("elytra").get("fireworkBoostSlot").getAsInt());
    }
}
