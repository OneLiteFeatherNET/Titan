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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.onelitefeather.titan.common.config.testing.CapturingLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link SectionBinder#bind(String, String, Class, Record, JsonElement)}'s handling of a
 * section that carries a key its record type does not declare - e.g. a leftover {@code
 * elytra.fireworkBoostSlot} left behind by an older file that used to have it (note: {@link
 * ElytraTestConfig}, this module's own stand-in, declares {@code boostMultiplier}, not {@code
 * fireworkBoostSlot} - the fixtures below are careful to only use keys neither test record
 * actually declares). These keys are never removed here: {@link SectionBinder} only warns once
 * per {@link SectionBinder#bind} call, using the same {@link CapturingLoggerFactory} SLF4J test
 * provider {@link LegacyConfigMigrationTest} uses.
 */
class SectionBinderUnknownKeysTest {

    private final SectionBinder binder = new SectionBinder();

    @BeforeEach
    void clearLog() {
        CapturingLoggerFactory.clear();
    }

    @Test
    @DisplayName("A section with an unknown key logs one warning naming the file, the section and the key")
    void unknownKeyLogsOneWarningNamingFileSectionAndKey() {
        JsonElement existing = JsonParser.parseString("{\"boostMultiplier\": 1.0, \"fireworkBoostSlot\": 45}");

        binder.bind("app.json", "elytra", ElytraTestConfig.class, ElytraTestConfig.DEFAULTS, existing);

        boolean logged = CapturingLoggerFactory.messages().stream().anyMatch(message -> message.contains("app.json") && message.contains("elytra") && message.contains("fireworkBoostSlot"));
        assertTrue(logged, "expected a warning naming app.json, elytra and fireworkBoostSlot, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("A declared key is never named as unknown")
    void declaredKeyIsNeverNamedAsUnknown() {
        JsonElement existing = JsonParser.parseString("{\"boostMultiplier\": 1.0, \"fireworkBoostSlot\": 45}");

        binder.bind("app.json", "elytra", ElytraTestConfig.class, ElytraTestConfig.DEFAULTS, existing);

        boolean declaredFieldWronglyNamed = CapturingLoggerFactory.messages().stream().anyMatch(message -> message.contains("boostMultiplier"));
        assertFalse(declaredFieldWronglyNamed, "boostMultiplier is declared by ElytraTestConfig and must never be flagged, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("Multiple unknown keys in the same section are all named in a single warning")
    void multipleUnknownKeysAreNamedInASingleWarning() {
        JsonElement existing = JsonParser.parseString("{\"boostMultiplier\": 1.0, \"fireworkBoostSlot\": 45, \"legacyFlag\": true}");

        binder.bind("app.json", "elytra", ElytraTestConfig.class, ElytraTestConfig.DEFAULTS, existing);

        long warningsForThisSection = CapturingLoggerFactory.messages().stream().filter(message -> message.contains("elytra") && message.contains("fireworkBoostSlot") && message.contains("legacyFlag")).count();
        assertEquals(1, warningsForThisSection, "both unknown keys must be named in exactly one warning, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("A section with only declared keys logs no warning")
    void sectionWithOnlyDeclaredKeysLogsNoWarning() {
        JsonElement existing = JsonParser.parseString("{\"cooldownMillis\": 9000}");

        binder.bind("app.json", "tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS, existing);

        assertTrue(CapturingLoggerFactory.messages().isEmpty(), "no unknown key must not log anything, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("A missing section logs no warning")
    void missingSectionLogsNoWarning() {
        binder.bind("app.json", "tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS, null);

        assertTrue(CapturingLoggerFactory.messages().isEmpty(), "a missing section has no unknown keys to warn about, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("A second, unrelated section's unknown key does not appear in the first section's warning")
    void unrelatedSectionsUnknownKeyDoesNotLeakIntoThisWarning() {
        JsonElement elytraExisting = JsonParser.parseString("{\"boostMultiplier\": 1.0, \"fireworkBoostSlot\": 45}");
        JsonElement tickleExisting = JsonParser.parseString("{\"cooldownMillis\": 9000, \"leftoverKey\": true}");

        binder.bind("app.json", "elytra", ElytraTestConfig.class, ElytraTestConfig.DEFAULTS, elytraExisting);
        binder.bind("app.json", "tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS, tickleExisting);

        boolean elytraWarningMentionsTickleKey = CapturingLoggerFactory.messages().stream().anyMatch(message -> message.contains("fireworkBoostSlot") && message.contains("leftoverKey"));
        assertFalse(elytraWarningMentionsTickleKey, "the elytra warning must not name tickle's own unknown key, log was: " + CapturingLoggerFactory.messages());
    }
}
