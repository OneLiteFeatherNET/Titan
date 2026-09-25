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
import net.onelitefeather.titan.app.feature.elytra.ElytraConfig;
import net.onelitefeather.titan.app.feature.navigator.NavigatorConfig;
import net.onelitefeather.titan.app.feature.tickle.TickleConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Characterization test for {@code avaje-config-facade} task 1.1: locks in that the classpath
 * {@code app/src/main/resources/application.yaml} - the file every module will read directly
 * through {@code io.avaje.config.Config} once the facade migration is complete - carries exactly
 * today's {@code DEFAULTS} of the {@code sit} and {@code spawn} sections (their config records
 * are gone, see {@code avaje-config-facade} task 2.1/2.2), plus {@link TickleConfig},
 * {@link ElytraConfig} and {@link NavigatorConfig}, key by key.
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

    @DisplayName("navigator: application.yaml matches NavigatorConfig.DEFAULTS")
    @Test
    void navigatorMatchesDefaults() {
        Configuration configuration = load();

        Assertions.assertEquals(NavigatorConfig.DEFAULTS.title(), configuration.get("navigator.title"), "navigator.title");

        Map<String, NavigatorConfig.Entry> expectedEntries = NavigatorConfig.DEFAULTS.entries();
        Assertions.assertEquals(expectedEntries.keySet(), configuration.forPath("navigator.entries").keys().stream().map(key -> key.split("\\.")[0]).collect(java.util.stream.Collectors.toSet()), "navigator.entries names");

        for (Map.Entry<String, NavigatorConfig.Entry> entry : expectedEntries.entrySet()) {
            String name = entry.getKey();
            NavigatorConfig.Entry expected = entry.getValue();
            String prefix = "navigator.entries." + name + ".";

            Assertions.assertEquals(expected.slot(), configuration.getInt(prefix + "slot"), prefix + "slot");
            Assertions.assertEquals(expected.icon(), configuration.get(prefix + "icon"), prefix + "icon");
            Assertions.assertEquals(expected.displayName(), configuration.get(prefix + "displayName"), prefix + "displayName");
            Assertions.assertEquals(expected.destination(), configuration.get(prefix + "destination"), prefix + "destination");
            Assertions.assertEquals(expected.feature(), configuration.getNullable(prefix + "feature"), prefix + "feature");
        }
    }
}
