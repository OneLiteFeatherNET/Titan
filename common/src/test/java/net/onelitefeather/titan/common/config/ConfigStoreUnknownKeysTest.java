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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link ConfigStore#section(String, Class, Record)}'s handling of a section that carries a
 * key its record type does not declare - e.g. a leftover {@code elytra.fireworkBoostSlot} left
 * behind by an older file that used to have it (note: {@link ElytraTestConfig}, this module's own
 * stand-in, declares {@code boostMultiplier}, not {@code fireworkBoostSlot} - the fixtures below
 * are careful to only use keys neither test record actually declares). Unlike {@link
 * LegacyConfigMigrationTest}'s dropped legacy keys, these keys are never removed: {@link
 * ConfigStore} only warns once per {@link ConfigStore#section} call, using the same {@link
 * CapturingLoggerFactory} SLF4J test provider {@link LegacyConfigMigrationTest} uses.
 */
class ConfigStoreUnknownKeysTest {

    @BeforeEach
    void clearLog() {
        CapturingLoggerFactory.clear();
    }

    @Test
    @DisplayName("A section with an unknown key logs one warning naming the file, the section and the key")
    void unknownKeyLogsOneWarningNamingFileSectionAndKey(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        Files.writeString(file, """
                {
                  "configVersion": 2,
                  "elytra": {"boostMultiplier": 1.0, "fireworkBoostSlot": 45}
                }
                """);
        ConfigStore store = ConfigStore.open(file);

        store.section("elytra", ElytraTestConfig.class, ElytraTestConfig.DEFAULTS);

        boolean logged = CapturingLoggerFactory.messages().stream().anyMatch(message -> message.contains("app.json") && message.contains("elytra") && message.contains("fireworkBoostSlot"));
        assertTrue(logged, "expected a warning naming app.json, elytra and fireworkBoostSlot, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("A declared key is never named as unknown")
    void declaredKeyIsNeverNamedAsUnknown(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        Files.writeString(file, """
                {
                  "configVersion": 2,
                  "elytra": {"boostMultiplier": 1.0, "fireworkBoostSlot": 45}
                }
                """);
        ConfigStore store = ConfigStore.open(file);

        store.section("elytra", ElytraTestConfig.class, ElytraTestConfig.DEFAULTS);

        boolean declaredFieldWronglyNamed = CapturingLoggerFactory.messages().stream().anyMatch(message -> message.contains("boostMultiplier"));
        assertFalse(declaredFieldWronglyNamed, "boostMultiplier is declared by ElytraTestConfig and must never be flagged, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("Multiple unknown keys in the same section are all named in a single warning")
    void multipleUnknownKeysAreNamedInASingleWarning(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        Files.writeString(file, """
                {
                  "configVersion": 2,
                  "elytra": {"boostMultiplier": 1.0, "fireworkBoostSlot": 45, "legacyFlag": true}
                }
                """);
        ConfigStore store = ConfigStore.open(file);

        store.section("elytra", ElytraTestConfig.class, ElytraTestConfig.DEFAULTS);

        long warningsForThisSection = CapturingLoggerFactory.messages().stream().filter(message -> message.contains("elytra") && message.contains("fireworkBoostSlot") && message.contains("legacyFlag")).count();
        assertEquals(1, warningsForThisSection, "both unknown keys must be named in exactly one warning, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("A section with only declared keys logs no warning")
    void sectionWithOnlyDeclaredKeysLogsNoWarning(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        Files.writeString(file, """
                {
                  "configVersion": 2,
                  "tickle": {"cooldownMillis": 9000}
                }
                """);
        ConfigStore store = ConfigStore.open(file);

        store.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS);

        assertTrue(CapturingLoggerFactory.messages().isEmpty(), "no unknown key must not log anything, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("A section missing from the file entirely logs no warning")
    void missingSectionLogsNoWarning(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        Files.writeString(file, """
                {
                  "configVersion": 2
                }
                """);
        ConfigStore store = ConfigStore.open(file);

        store.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS);

        assertTrue(CapturingLoggerFactory.messages().isEmpty(), "a missing section has no unknown keys to warn about, log was: " + CapturingLoggerFactory.messages());
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

    @Test
    @DisplayName("A second, unrelated section's unknown key does not appear in the first section's warning")
    void unrelatedSectionsUnknownKeyDoesNotLeakIntoThisWarning(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        Files.writeString(file, """
                {
                  "configVersion": 2,
                  "elytra": {"boostMultiplier": 1.0, "fireworkBoostSlot": 45},
                  "tickle": {"cooldownMillis": 9000, "leftoverKey": true}
                }
                """);
        ConfigStore store = ConfigStore.open(file);

        store.section("elytra", ElytraTestConfig.class, ElytraTestConfig.DEFAULTS);

        boolean elytraWarningMentionsTickleKey = CapturingLoggerFactory.messages().stream().anyMatch(message -> message.contains("fireworkBoostSlot") && message.contains("leftoverKey"));
        assertFalse(elytraWarningMentionsTickleKey, "the elytra warning must not name tickle's own unknown key, log was: " + CapturingLoggerFactory.messages());
    }
}
