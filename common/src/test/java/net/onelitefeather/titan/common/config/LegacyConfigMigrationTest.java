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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link LegacyConfigMigration#toSectioned(JsonObject, String)} directly: the mapping
 * itself (see {@code design.md}, decision 5 of the {@code lobby-feature-modules} change, and the
 * {@code lobby-module-config} spec scenario "migration of the previous file") is exercised through
 * the real {@code app.json} fixture by {@link AppJsonMigrationTest#migratesFlatV1AppJson(java.nio.file.Path)},
 * so this class only covers the one failure case that test does not: a legacy
 * {@code allowedSitBlocks} entry missing its {@code value} key.
 */
class LegacyConfigMigrationTest {

    @Test
    @DisplayName("A legacy allowedSitBlocks entry missing 'value' fails with a clear ConfigException instead of NPE-ing")
    void allowedBlocksEntryMissingValueFailsClearly() {
        JsonObject legacy = JsonParser.parseString("""
                {
                  "allowedSitBlocks": [ {"namespace": "minecraft"} ]
                }
                """).getAsJsonObject();

        ConfigException exception = assertThrows(ConfigException.class, () -> LegacyConfigMigration.toSectioned(legacy, "app.json"));

        assertEquals("sit", exception.section());
        assertEquals("allowedBlocks", exception.field());
        assertTrue(exception.reason() != null && exception.reason().toLowerCase().contains("value"), "reason should mention the missing 'value' key: " + exception.reason());
        assertTrue(exception.getMessage().contains("app.json"), "message should name the file: " + exception.getMessage());
    }
}
