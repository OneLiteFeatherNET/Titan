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
package net.onelitefeather.titan.app.bootstrap;

import io.avaje.config.Configuration;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.app.feature.elytra.ElytraConfig;
import net.onelitefeather.titan.app.feature.sit.SitConfig;
import net.onelitefeather.titan.app.feature.spawn.SpawnConfig;
import net.onelitefeather.titan.app.feature.tickle.TickleConfig;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Characterization test for {@code avaje-config-facade} task 1.1: locks in that the classpath
 * {@code app/src/main/resources/application.yaml} - the file every module will read directly
 * through {@code io.avaje.config.Config} once the facade migration is complete - carries exactly
 * today's {@code DEFAULTS} of {@link SitConfig}, {@link SpawnConfig}, {@link TickleConfig} and
 * {@link ElytraConfig}, key by key, and the shipped navigator defaults (the navigator module's own
 * per-entry config record was removed once the navigator module moved onto this file, see {@code
 * openspec/changes/avaje-config-facade/design.md}, decision 6 - its navigator assertions below
 * compare against the same values, spelled out literally, instead).
 *
 * <p>Loads the file as its own {@link Configuration} instance via
 * {@link Configuration.Builder#load(String)} - which reads a classpath resource, never the static
 * {@code io.avaje.config.Config} facade (see design.md, decision 5: unit tests never touch that
 * facade) - so this test has no dependency on JVM-wide state and stays Independent and Repeatable
 * (F.I.R.S.T.).
 *
 * <p>Once the five config records above are removed in a later wave, this test's assertions move
 * to whatever locks in the shipped defaults at that point; until then it is the safety net that
 * {@code application.yaml} and the records it will replace never silently drift apart.
 */
class ApplicationYamlDefaultsCharacterizationTest {

    private static Configuration load() {
        return Configuration.builder().load("application.yaml").build();
    }

    @DisplayName("spawn: application.yaml matches SpawnConfig.DEFAULTS")
    @Test
    void spawnMatchesDefaults() {
        Configuration configuration = load();

        Assertions.assertEquals(SpawnConfig.DEFAULTS.minHeight(), configuration.getInt("spawn.minHeight"), "spawn.minHeight");
        Assertions.assertEquals(SpawnConfig.DEFAULTS.maxHeight(), configuration.getInt("spawn.maxHeight"), "spawn.maxHeight");
        Assertions.assertEquals(SpawnConfig.DEFAULTS.simulationDistance(), configuration.getInt("spawn.simulationDistance"), "spawn.simulationDistance");
    }

    @DisplayName("sit: application.yaml matches SitConfig.DEFAULTS")
    @Test
    void sitMatchesDefaults() {
        Configuration configuration = load();

        Vec expectedOffset = SitConfig.DEFAULTS.offset();
        Assertions.assertEquals(expectedOffset.x(), configuration.getDecimal("sit.offset.x").doubleValue(), "sit.offset.x");
        Assertions.assertEquals(expectedOffset.y(), configuration.getDecimal("sit.offset.y").doubleValue(), "sit.offset.y");
        Assertions.assertEquals(expectedOffset.z(), configuration.getDecimal("sit.offset.z").doubleValue(), "sit.offset.z");

        List<String> expectedAllowedBlocks = SitConfig.DEFAULTS.allowedBlocks().stream().map(Key::asString).toList();
        Assertions.assertEquals(expectedAllowedBlocks, configuration.list().of("sit.allowedBlocks"), "sit.allowedBlocks");
    }

    @DisplayName("tickle: application.yaml matches TickleConfig.DEFAULTS")
    @Test
    void tickleMatchesDefaults() {
        Configuration configuration = load();

        Assertions.assertEquals(TickleConfig.DEFAULTS.cooldownMillis(), configuration.getLong("tickle.cooldownMillis"), "tickle.cooldownMillis");
    }

    @DisplayName("elytra: application.yaml matches ElytraConfig.DEFAULTS")
    @Test
    void elytraMatchesDefaults() {
        Configuration configuration = load();

        Assertions.assertEquals(ElytraConfig.DEFAULTS.burnDurationTicks(), configuration.getInt("elytra.burnDurationTicks"), "elytra.burnDurationTicks");
        Assertions.assertEquals(ElytraConfig.DEFAULTS.cooldownTicks(), configuration.getInt("elytra.cooldownTicks"), "elytra.cooldownTicks");
    }

    @DisplayName("navigator: application.yaml matches the shipped defaults - ElytraRace, Survival, Slender (gated behind NAVIGATOR_SLENDER) and Creative")
    @Test
    void navigatorMatchesDefaults() {
        Configuration configuration = load();

        Assertions.assertEquals("<yellow>Navigator", configuration.get("navigator.title"), "navigator.title");

        Map<String, ExpectedNavigatorEntry> expectedEntries = Map.of("elytrarace", new ExpectedNavigatorEntry(0, "minecraft:elytra", "<!i><gradient:#fcba03:#03fc8c>ElytraRace</gradient>", "ElytraRace", null), "survival", new ExpectedNavigatorEntry(4, "minecraft:grass_block", "<!i><green>Survival", "Survival", null), "slender", new ExpectedNavigatorEntry(5, "minecraft:enderman_spawn_egg", "<!i><gradient:#616161:#e80000c>Slender</gradient>", "cygnus", "NAVIGATOR_SLENDER"), "creative", new ExpectedNavigatorEntry(8, "minecraft:wooden_axe", "<!i><rainbow>Creative</rainbow>", "MemberBuild", null));
        Assertions.assertEquals(expectedEntries.keySet(), configuration.forPath("navigator.entries").keys().stream().map(key -> key.split("\\.")[0]).collect(java.util.stream.Collectors.toSet()), "navigator.entries names");

        for (Map.Entry<String, ExpectedNavigatorEntry> entry : expectedEntries.entrySet()) {
            String name = entry.getKey();
            ExpectedNavigatorEntry expected = entry.getValue();
            String prefix = "navigator.entries." + name + ".";

            Assertions.assertEquals(expected.slot(), configuration.getInt(prefix + "slot"), prefix + "slot");
            Assertions.assertEquals(expected.icon(), configuration.get(prefix + "icon"), prefix + "icon");
            Assertions.assertEquals(expected.displayName(), configuration.get(prefix + "displayName"), prefix + "displayName");
            Assertions.assertEquals(expected.destination(), configuration.get(prefix + "destination"), prefix + "destination");
            Assertions.assertEquals(expected.feature(), configuration.getNullable(prefix + "feature"), prefix + "feature");
        }
    }

    /**
     * A literal stand-in for the navigator's former per-entry config record, spelling out one
     * expected default entry's fields so this test does not depend on the removed record (see
     * {@code openspec/changes/avaje-config-facade/design.md}, decision 6).
     */
    private record ExpectedNavigatorEntry(int slot, String icon, String displayName,
                                          String destination, @Nullable String feature) {
    }
}
