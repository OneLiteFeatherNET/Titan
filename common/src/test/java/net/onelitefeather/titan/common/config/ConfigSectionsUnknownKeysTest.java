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
import net.onelitefeather.titan.common.config.testing.CapturingLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link ConfigSections#section(String, Class, Record)}'s handling of a section that
 * carries a key its record type does not declare - e.g. a leftover {@code elytra.legacyFlag}.
 * Exactly one warning is logged per section, naming the section and every unknown key, captured
 * via the same {@link CapturingLoggerFactory} SLF4J test provider {@link
 * ConfigStoreUnknownKeysTest} uses.
 */
class ConfigSectionsUnknownKeysTest {

    @BeforeEach
    void clearLog() {
        CapturingLoggerFactory.clear();
    }

    @Test
    @DisplayName("A section with unknown keys logs exactly one warning naming the section and the keys")
    void unknownKeysLogExactlyOneWarningNamingSectionAndKeys() {
        Configuration configuration = Configuration.builder().putAll(Map.of(
                "elytra.boostMultiplier", "1.0", "elytra.legacyFlag", "true"
        )).build();
        ConfigSections sections = new ConfigSections(configuration);

        sections.section("elytra", ElytraTestConfig.class, ElytraTestConfig.DEFAULTS);

        long warningsForThisSection = CapturingLoggerFactory.messages().stream().filter(message -> message.contains("elytra") && message.contains("legacyFlag")).count();
        assertEquals(1, warningsForThisSection, "expected exactly one warning naming elytra and legacyFlag, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("A declared key is never named as unknown")
    void declaredKeyIsNeverNamedAsUnknown() {
        Configuration configuration = Configuration.builder().putAll(Map.of(
                "elytra.boostMultiplier", "1.0", "elytra.legacyFlag", "true"
        )).build();
        ConfigSections sections = new ConfigSections(configuration);

        sections.section("elytra", ElytraTestConfig.class, ElytraTestConfig.DEFAULTS);

        boolean declaredFieldWronglyNamed = CapturingLoggerFactory.messages().stream().anyMatch(message -> message.contains("boostMultiplier"));
        assertTrue(!declaredFieldWronglyNamed, "boostMultiplier is declared by ElytraTestConfig and must never be flagged, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("A section with only declared keys logs no warning")
    void sectionWithOnlyDeclaredKeysLogsNoWarning() {
        Configuration configuration = Configuration.builder().putAll(Map.of("tickle.cooldownMillis", "9000")).build();
        ConfigSections sections = new ConfigSections(configuration);

        sections.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS);

        assertTrue(CapturingLoggerFactory.messages().isEmpty(), "no unknown key must not log anything, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("A section missing from the configuration entirely logs no warning")
    void missingSectionLogsNoWarning() {
        Configuration configuration = Configuration.builder().build();
        ConfigSections sections = new ConfigSections(configuration);

        sections.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS);

        assertTrue(CapturingLoggerFactory.messages().isEmpty(), "a missing section has no unknown keys to warn about, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("An unknown key nested inside a record field is reported with its full dotted path")
    void unknownKeyNestedInsideRecordFieldIsReportedWithDottedPath() {
        Configuration configuration = Configuration.builder().putAll(Map.of("sit.offset.xx", "5")).build();
        ConfigSections sections = new ConfigSections(configuration);

        sections.section("sit", SitTestConfig.class, SitTestConfig.DEFAULTS);

        long warningsNamingTheNestedKey = CapturingLoggerFactory.messages().stream().filter(message -> message.contains("sit") && message.contains("offset.xx")).count();
        assertEquals(1, warningsNamingTheNestedKey, "expected exactly one warning naming sit and offset.xx, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("An unknown key nested inside one entry of a map-of-records field is reported with its full dotted path")
    void unknownKeyNestedInsideMapOfRecordsEntryIsReportedWithDottedPath() {
        Configuration configuration = Configuration.builder().putAll(Map.of("navigator.entries.survival.slott", "3")).build();
        ConfigSections sections = new ConfigSections(configuration);

        sections.section("navigator", NavigatorTestConfig.class, NavigatorTestConfig.DEFAULTS);

        long warningsNamingTheNestedKey = CapturingLoggerFactory.messages().stream().filter(message -> message.contains("navigator") && message.contains("entries.survival.slott")).count();
        assertEquals(1, warningsNamingTheNestedKey, "expected exactly one warning naming navigator and entries.survival.slott, log was: " + CapturingLoggerFactory.messages());
    }

    @Test
    @DisplayName("A new entry key under a map-of-records field is never reported as unknown")
    void newEntryKeyUnderMapOfRecordsFieldIsNeverReportedAsUnknown() {
        Configuration configuration = Configuration.builder().putAll(Map.of(
                "navigator.entries.parkour.slot", "2", "navigator.entries.parkour.destination", "parkour-lobby"
        )).build();
        ConfigSections sections = new ConfigSections(configuration);

        sections.section("navigator", NavigatorTestConfig.class, NavigatorTestConfig.DEFAULTS);

        assertTrue(CapturingLoggerFactory.messages().isEmpty(), "a new, well-formed map entry must not be flagged as unknown, log was: " + CapturingLoggerFactory.messages());
    }
}
