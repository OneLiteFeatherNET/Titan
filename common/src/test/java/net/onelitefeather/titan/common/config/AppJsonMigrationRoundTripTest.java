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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.avaje.config.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the round trip {@code design.md} decision 4 promises and the {@code lobby-module-config}
 * spec requirement "Altes flaches Format wird automatisch migriert": the {@code application.yaml}
 * {@link AppJsonMigration} writes must be readable by the real {@code avaje-config} {@link
 * Configuration} pipeline (SnakeYAML flattening a nested map, a list of scalars and - since
 * {@code avaje-config} only hands out flat keys - re-flattening a JSON object back into one), and
 * {@link ConfigSections} must rebuild the exact same records from those flat keys that a direct
 * {@link SectionBinder} bind over the original (pre-migration) JSON tree would produce.
 * <p>
 * Only sections whose module record has a same-shaped stand-in in {@code common}'s own test tree
 * (see {@link SitTestConfig}, {@link SpawnTestConfig}, {@link TickleTestConfig}, {@link
 * ElytraTestConfig}) are bound into a record here; {@code navigator} is compared by its flat keys
 * instead, because its real shape in these fixtures is a JSON <em>list</em> of entries, while
 * {@link NavigatorTestConfig} - this module's own stand-in for the shape decision 3 chose - is
 * keyed by a <em>map</em>, so it cannot bind the fixtures' navigator section at all.
 */
class AppJsonMigrationRoundTripTest {

    private final AppJsonMigration migration = new AppJsonMigration();
    private final SectionBinder binder = new SectionBinder();

    @Test
    @DisplayName("A migrated v2 app.json binds into the same records through the real YAML pipeline as the original JSON bound directly")
    void migratedV2AppJsonBindsToSameRecordsAsOriginalJsonThroughRealYamlPipeline(@TempDir Path tempDir) throws IOException {
        Path appJson = tempDir.resolve("app.json");
        copyFixture("/config/v2/app.json", appJson);
        JsonObject originalRoot = parseFixture("/config/v2/app.json");

        migration.migrate(tempDir);

        Configuration configuration = loadMigratedConfiguration(tempDir);
        ConfigSections sections = new ConfigSections(configuration);

        assertSectionRoundTrips(sections, originalRoot, "sit", SitTestConfig.class, SitTestConfig.DEFAULTS);
        assertSectionRoundTrips(sections, originalRoot, "spawn", SpawnTestConfig.class, SpawnTestConfig.DEFAULTS);
        assertSectionRoundTrips(sections, originalRoot, "tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS);

        Configuration navigatorScope = configuration.forPath("navigator");
        assertEquals("<yellow>Navigator", navigatorScope.get("title"), "navigator.title must survive the YAML round trip");
        assertEquals("0", navigatorScope.get("entries[0].slot"), "the first navigator entry's slot must survive the YAML round trip");
        assertEquals("ElytraRace", navigatorScope.get("entries[0].destination"), "the first navigator entry's destination must survive the YAML round trip");
        assertEquals("5", navigatorScope.get("entries[2].slot"), "the third navigator entry's slot must survive the YAML round trip");
        assertEquals("cygnus", navigatorScope.get("entries[2].destination"), "the third navigator entry's destination must survive the YAML round trip");
        assertEquals("8", navigatorScope.get("entries[3].slot"), "the fourth navigator entry's slot must survive the YAML round trip");
    }

    @Test
    @DisplayName("A migrated legacy v1 app.json binds into the same records through the real YAML pipeline as its migrated JSON tree bound directly")
    void migratedLegacyAppJsonBindsToSameRecordsAsMigratedJsonThroughRealYamlPipeline(@TempDir Path tempDir) throws IOException {
        Path appJson = tempDir.resolve("app.json");
        copyFixture("/config/legacy/app.json", appJson);
        JsonObject legacyRoot = parseFixture("/config/legacy/app.json");
        JsonObject sectionedRoot = LegacyConfigMigration.toSectioned(legacyRoot, "app.json");

        migration.migrate(tempDir);

        Configuration configuration = loadMigratedConfiguration(tempDir);
        ConfigSections sections = new ConfigSections(configuration);

        assertSectionRoundTrips(sections, sectionedRoot, "sit", SitTestConfig.class, SitTestConfig.DEFAULTS);
        assertSectionRoundTrips(sections, sectionedRoot, "spawn", SpawnTestConfig.class, SpawnTestConfig.DEFAULTS);
        assertSectionRoundTrips(sections, sectionedRoot, "tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS);
        // The legacy migration drops elytraBoostMultiplier entirely (design.md decision 7 of
        // lobby-feature-modules): no "elytra" section is migrated at all, so both a direct bind
        // over the (missing) section and the real YAML pipeline must fall back to the module's
        // own defaults.
        assertSectionRoundTrips(sections, sectionedRoot, "elytra", ElytraTestConfig.class, ElytraTestConfig.DEFAULTS);
    }

    private <R extends Record> void assertSectionRoundTrips(ConfigSections sections, JsonObject originalRoot, String id, Class<R> type, R defaults) {
        JsonElement directSource = originalRoot.has(id) ? originalRoot.get(id) : null;
        R expected = binder.bind(null, id, type, defaults, directSource).value();
        R actual = sections.section(id, type, defaults);
        assertEquals(expected, actual, id + " must bind to the same record through the real YAML pipeline as through a direct SectionBinder bind over the original JSON");
    }

    private static Configuration loadMigratedConfiguration(Path tempDir) {
        Path applicationYaml = tempDir.resolve("application.yaml");
        return Configuration.builder().load(applicationYaml.toFile()).build();
    }

    private static JsonObject parseFixture(String resource) throws IOException {
        try (InputStream in = AppJsonMigrationRoundTripTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Test fixture " + resource + " is missing from the test resources");
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static void copyFixture(String resource, Path target) throws IOException {
        try (InputStream in = AppJsonMigrationRoundTripTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Test fixture " + resource + " is missing from the test resources");
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
