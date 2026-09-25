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
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locks in that the classpath {@code app/src/main/resources/application.yaml} - the single source
 * of every module's shipped defaults, read directly through {@code io.avaje.config.Config} (see
 * {@code openspec/changes/avaje-config-facade/design.md}, decision 2) - carries exactly the
 * expected defaults of the {@code sit}, {@code spawn}, {@code tickle}, {@code elytra} and
 * {@code navigator} sections, key by key. Each module's own per-field configuration type is gone
 * (see design.md, decision 7), so the expected values are spelled out literally here instead of
 * compared against one.
 *
 * <p>Loads the file as its own {@link Configuration} instance via
 * {@link Configuration.Builder#load(String)} - which reads a classpath resource, never the static
 * {@code io.avaje.config.Config} facade (see design.md, decision 5: unit tests never touch that
 * facade) - so this test has no dependency on JVM-wide state and stays Independent and Repeatable
 * (F.I.R.S.T.).
 *
 * <p>This is the safety net that a shipped default cannot silently drift out from under whichever
 * module reads it - the only other place any of these values could be found is the
 * {@code lobby-module-config} spec's own examples, which this test does not read.
 */
class ApplicationYamlDefaultsCharacterizationTest {

    private static Configuration load() {
        return Configuration.builder().load("application.yaml").build();
    }

    @DisplayName("spawn: application.yaml matches the shipped defaults (-64, 310, 2)")
    @Test
    void spawnMatchesDefaults() {
        Configuration configuration = load();

        Assertions.assertEquals(-64, configuration.getInt("spawn.minHeight"), "spawn.minHeight");
        Assertions.assertEquals(310, configuration.getInt("spawn.maxHeight"), "spawn.maxHeight");
        Assertions.assertEquals(2, configuration.getInt("spawn.simulationDistance"), "spawn.simulationDistance");
    }

    @DisplayName("sit: application.yaml matches the shipped defaults")
    @Test
    void sitMatchesDefaults() {
        Configuration configuration = load();

        Assertions.assertEquals(0.5, configuration.getDecimal("sit.offset.x").doubleValue(), "sit.offset.x");
        Assertions.assertEquals(0.25, configuration.getDecimal("sit.offset.y").doubleValue(), "sit.offset.y");
        Assertions.assertEquals(0.5, configuration.getDecimal("sit.offset.z").doubleValue(), "sit.offset.z");

        Assertions.assertEquals(List.of("minecraft:spruce_stairs"), configuration.list().of("sit.allowedBlocks"), "sit.allowedBlocks");
    }

    @DisplayName("tickle: application.yaml matches today's shipped default (4000ms)")
    @Test
    void tickleMatchesDefaults() {
        Configuration configuration = load();

        Assertions.assertEquals(4000L, configuration.getLong("tickle.cooldownMillis"), "tickle.cooldownMillis");
    }

    @DisplayName("elytra: application.yaml matches today's shipped defaults (30 / 40 ticks)")
    @Test
    void elytraMatchesDefaults() {
        Configuration configuration = load();

        Assertions.assertEquals(30, configuration.getInt("elytra.burnDurationTicks"), "elytra.burnDurationTicks");
        Assertions.assertEquals(40, configuration.getInt("elytra.cooldownTicks"), "elytra.cooldownTicks");
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
     * A literal stand-in for one expected default navigator entry's fields, spelling them out
     * instead of comparing against a type, since navigator entries are no longer bound to one (see
     * {@code openspec/changes/avaje-config-facade/design.md}, decision 6).
     */
    private record ExpectedNavigatorEntry(int slot, String icon, String displayName,
                                          String destination, @Nullable String feature) {
    }
}
