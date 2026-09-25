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

import net.onelitefeather.titan.common.config.testing.CapturingLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@code lobby-module-config} spec requirement "Bestehende app.json wird einmalig
 * umgestellt": {@link AppJsonMigration#migrate(Path)} turns a legacy {@code app.json} (flat v1 or
 * sectioned v2) into {@code application.yaml} exactly once, renames the original to {@code
 * app.json.migrated}, and never touches either file when {@code application.yaml} already exists.
 */
class AppJsonMigrationTest {

    private final AppJsonMigration migration = new AppJsonMigration();
    private final Yaml yaml = new Yaml();

    @BeforeEach
    void clearLog() {
        CapturingLoggerFactory.clear();
    }

    @Test
    @DisplayName("A flat v1 app.json is migrated to application.yaml, the original is renamed, and dropped keys are logged")
    void migratesFlatV1AppJson(@TempDir Path tempDir) throws IOException {
        Path appJson = tempDir.resolve("app.json");
        copyFixture("/config/legacy/app.json", appJson);

        migration.migrate(tempDir);

        Path applicationYaml = tempDir.resolve("application.yaml");
        Path migratedAppJson = tempDir.resolve("app.json.migrated");
        assertTrue(Files.exists(applicationYaml), "application.yaml must be written");
        assertTrue(Files.exists(migratedAppJson), "app.json must be renamed to app.json.migrated");
        assertFalse(Files.exists(appJson), "app.json must no longer exist under its original name");

        Object written = loadYaml(applicationYaml);
        assertTrue(written instanceof java.util.Map, "application.yaml must contain a mapping");
        @SuppressWarnings("unchecked") var root = (java.util.Map<String, Object>) written;

        @SuppressWarnings("unchecked") var tickle = (java.util.Map<String, Object>) root.get("tickle");
        assertEquals(4000, tickle.get("cooldownMillis"), "tickleDuration must become tickle.cooldownMillis");

        @SuppressWarnings("unchecked") var spawn = (java.util.Map<String, Object>) root.get("spawn");
        assertEquals(-64, spawn.get("minHeight"));
        assertEquals(310, spawn.get("maxHeight"));
        assertEquals(2, spawn.get("simulationDistance"));

        @SuppressWarnings("unchecked") var sit = (java.util.Map<String, Object>) root.get("sit");
        assertTrue(sit.containsKey("offset"), "sitOffset must become sit.offset");
        assertTrue(sit.containsKey("allowedBlocks"), "allowedSitBlocks must become sit.allowedBlocks");

        assertFalse(root.containsKey("elytraBoostMultiplier"), "dropped keys must not survive migration");
        assertFalse(root.containsKey("updateRateAgones"), "dropped keys must not survive migration");
        assertFalse(root.containsKey("fireworkBoostSlot"), "dropped keys must not survive migration");

        boolean loggedDroppedKeys = CapturingLoggerFactory.messages().stream().anyMatch(message -> message.contains("fireworkBoostSlot") && message.contains("updateRateAgones") && message.contains("elytraBoostMultiplier"));
        assertTrue(loggedDroppedKeys, "dropped legacy keys must be named in a WARN log, log was: " + CapturingLoggerFactory.messages());

        boolean loggedMigration = CapturingLoggerFactory.messages().stream().anyMatch(message -> message.startsWith("WARN") && message.contains("app.json.migrated"));
        assertTrue(loggedMigration, "the migration itself must be logged with a WARN naming app.json.migrated, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("A sectioned v2 app.json is migrated to application.yaml with the same sections and values")
    void migratesSectionedV2AppJson(@TempDir Path tempDir) throws IOException {
        Path appJson = tempDir.resolve("app.json");
        copyFixture("/config/v2/app.json", appJson);
        String originalContent = Files.readString(appJson);

        migration.migrate(tempDir);

        Path applicationYaml = tempDir.resolve("application.yaml");
        assertTrue(Files.exists(applicationYaml), "application.yaml must be written");
        assertTrue(Files.exists(tempDir.resolve("app.json.migrated")), "app.json must be renamed to app.json.migrated");

        @SuppressWarnings("unchecked") var expectedRoot = (java.util.Map<String, Object>) yaml.load(originalContent);
        @SuppressWarnings("unchecked") var expectedNavigator = (java.util.Map<String, Object>) expectedRoot.remove("navigator");
        @SuppressWarnings("unchecked") var actualRoot = (java.util.Map<String, Object>) loadYaml(applicationYaml);
        @SuppressWarnings("unchecked") var actualNavigator = (java.util.Map<String, Object>) actualRoot.remove("navigator");

        assertEquals(expectedRoot, actualRoot, "every non-navigator section must contain the same values as the original app.json");
        assertEquals(expectedNavigator.get("title"), actualNavigator.get("title"), "navigator.title must survive the migration");
    }

    @Test
    @DisplayName("A sectioned v2 app.json's navigator.entries list is migrated to a map keyed by name derived from displayName")
    void migratesNavigatorEntriesListToMapKeyedByName(@TempDir Path tempDir) throws IOException {
        Path appJson = tempDir.resolve("app.json");
        copyFixture("/config/v2/app.json", appJson);

        migration.migrate(tempDir);

        Path applicationYaml = tempDir.resolve("application.yaml");
        @SuppressWarnings("unchecked") var root = (java.util.Map<String, Object>) loadYaml(applicationYaml);
        @SuppressWarnings("unchecked") var navigator = (java.util.Map<String, Object>) root.get("navigator");
        @SuppressWarnings("unchecked") var entries = (java.util.Map<String, Object>) navigator.get("entries");

        assertEquals(java.util.Set.of("elytrarace", "survival", "slender", "creative"), entries.keySet(), "the v2 fixture's four entries must be named exactly elytrarace, survival, slender and creative");

        @SuppressWarnings("unchecked") var elytrarace = (java.util.Map<String, Object>) entries.get("elytrarace");
        assertEquals(0, elytrarace.get("slot"));
        assertEquals("ElytraRace", elytrarace.get("destination"));

        @SuppressWarnings("unchecked") var slender = (java.util.Map<String, Object>) entries.get("slender");
        assertEquals(5, slender.get("slot"));
        assertEquals("cygnus", slender.get("destination"));
        assertEquals("NAVIGATOR_SLENDER", slender.get("feature"));

        @SuppressWarnings("unchecked") var creative = (java.util.Map<String, Object>) entries.get("creative");
        assertEquals(8, creative.get("slot"));
    }

    @Test
    @DisplayName("When application.yaml already exists, app.json is left untouched and only a warning is logged")
    void bothFilesPresentLeavesAppJsonUntouched(@TempDir Path tempDir) throws IOException {
        Path appJson = tempDir.resolve("app.json");
        copyFixture("/config/v2/app.json", appJson);
        String originalAppJson = Files.readString(appJson);

        Path applicationYaml = tempDir.resolve("application.yaml");
        String originalYaml = "tickle:\n  cooldownMillis: 1234\n";
        Files.writeString(applicationYaml, originalYaml);

        migration.migrate(tempDir);

        assertEquals(originalAppJson, Files.readString(appJson), "app.json must not be modified when application.yaml already exists");
        assertEquals(originalYaml, Files.readString(applicationYaml), "an existing application.yaml must not be modified");
        assertFalse(Files.exists(tempDir.resolve("app.json.migrated")), "app.json must not be renamed when application.yaml already exists");

        boolean loggedIgnored = CapturingLoggerFactory.messages().stream().anyMatch(message -> message.startsWith("WARN") && message.contains("app.json") && message.toLowerCase().contains("ignor"));
        assertTrue(loggedIgnored, "a WARN must say that app.json is ignored, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("Without either file, nothing happens and no file is created")
    void neitherFilePresentDoesNothing(@TempDir Path tempDir) throws IOException {
        migration.migrate(tempDir);

        try (var entries = Files.list(tempDir)) {
            assertTrue(entries.findAny().isEmpty(), "no file must be created when neither app.json nor application.yaml exists");
        }
    }

    @Test
    @DisplayName("Broken JSON fails with the file name and error position, and nothing is renamed or written")
    void brokenJsonFailsClearlyWithoutSideEffects(@TempDir Path tempDir) throws IOException {
        Path appJson = tempDir.resolve("app.json");
        String broken = "{ \"tickle\": { \"cooldownMillis\": 4000 \"extra\": 1 } }"; // missing comma
        Files.writeString(appJson, broken);

        ConfigException exception = assertThrows(ConfigException.class, () -> migration.migrate(tempDir));

        assertTrue(exception.getMessage().contains("app.json"), "message should name the file: " + exception.getMessage());
        assertTrue(exception.getMessage().toLowerCase().contains("line") && exception.getMessage().toLowerCase().contains("column"), "message should name the parser position: " + exception.getMessage());

        assertEquals(broken, Files.readString(appJson), "a broken app.json must never be modified");
        assertFalse(Files.exists(tempDir.resolve("app.json.migrated")), "a broken app.json must never be renamed");
        assertFalse(Files.exists(tempDir.resolve("application.yaml")), "no application.yaml may be written for a broken app.json");
    }

    private Object loadYaml(Path file) throws IOException {
        try (var reader = Files.newBufferedReader(file)) {
            return yaml.load(reader);
        }
    }

    private static void copyFixture(String resource, Path target) throws IOException {
        try (InputStream in = AppJsonMigrationTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Test fixture " + resource + " is missing from the test resources");
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
