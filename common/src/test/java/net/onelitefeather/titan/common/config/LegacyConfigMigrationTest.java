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
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.common.config.testing.CapturingLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migrates the real {@code app.json} from the repository root (copied to {@code
 * common/src/test/resources/config/legacy/app.json}) and checks the resulting sectioned values,
 * the backup file, and that the dropped legacy keys are logged - see {@code design.md}, decision 5
 * of the {@code lobby-feature-modules} change, and the {@code lobby-module-config} spec scenario
 * "migration of the previous file".
 */
class LegacyConfigMigrationTest {

    @BeforeEach
    void clearLog() {
        CapturingLoggerFactory.clear();
    }

    @Test
    @DisplayName("Migrating the repository's app.json produces the expected sections, a backup and a log line")
    void migratesTheRealRepoAppJson(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        copyLegacyFixture(file);
        String original = Files.readString(file);

        ConfigStore store = ConfigStore.open(file);

        Path backup = tempDir.resolve("app.json.v1.bak");
        assertTrue(Files.exists(backup), "the original file must be backed up before migration");
        assertEquals(original, Files.readString(backup), "the backup must be an exact copy of the original");

        TickleTestConfig tickle = store.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS);
        assertEquals(4000L, tickle.cooldownMillis());

        SpawnTestConfig spawn = store.section("spawn", SpawnTestConfig.class, SpawnTestConfig.DEFAULTS);
        assertEquals(-64, spawn.minHeight());
        assertEquals(310, spawn.maxHeight());
        assertEquals(2, spawn.simulationDistance());

        SitTestConfig sit = store.section("sit", SitTestConfig.class, SitTestConfig.DEFAULTS);
        assertEquals(new Vec(0.5, 0.25, 0.5), sit.offset());
        assertEquals(List.of(Key.key("minecraft:spruce_stairs")), sit.allowedBlocks());

        ElytraTestConfig elytra = store.section("elytra", ElytraTestConfig.class, ElytraTestConfig.DEFAULTS);
        assertEquals(ElytraTestConfig.DEFAULTS.boostMultiplier(), elytra.boostMultiplier(), "elytraBoostMultiplier has no equivalent in the ported Voyager boost and is dropped, not migrated - the section falls back to its defaults");

        boolean loggedDroppedKeys = CapturingLoggerFactory.messages().stream().anyMatch(message -> message.contains("fireworkBoostSlot") && message.contains("updateRateAgones") && message.contains("elytraBoostMultiplier"));
        assertTrue(loggedDroppedKeys, "dropped legacy keys must be logged, log was: " + CapturingLoggerFactory.messages());

        store.flush();
        JsonObject migrated = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertEquals(2, migrated.get("configVersion").getAsInt());
        assertFalse(migrated.has("fireworkBoostSlot"));
        assertFalse(migrated.has("updateRateAgones"));
        assertFalse(migrated.has("elytraBoostMultiplier"));
        assertFalse(migrated.has("tickleDuration"), "legacy flat keys must not survive migration");
    }

    @Test
    @DisplayName("A legacy allowedSitBlocks entry missing 'value' fails with a clear ConfigException instead of NPE-ing")
    void allowedBlocksEntryMissingValueFailsClearly(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        Files.writeString(file, """
                {
                  "allowedSitBlocks": [ {"namespace": "minecraft"} ]
                }
                """);

        ConfigException exception = assertThrows(ConfigException.class, () -> ConfigStore.open(file));

        assertEquals("sit", exception.section());
        assertEquals("allowedBlocks", exception.field());
        assertTrue(exception.reason() != null && exception.reason().toLowerCase().contains("value"), "reason should mention the missing 'value' key: " + exception.reason());
        assertTrue(exception.getMessage().contains("app.json"), "message should name the file: " + exception.getMessage());
    }

    private static void copyLegacyFixture(Path target) throws IOException {
        try (InputStream in = LegacyConfigMigrationTest.class.getResourceAsStream("/config/legacy/app.json")) {
            if (in == null) {
                throw new IOException("Test fixture /config/legacy/app.json is missing from the test resources");
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
