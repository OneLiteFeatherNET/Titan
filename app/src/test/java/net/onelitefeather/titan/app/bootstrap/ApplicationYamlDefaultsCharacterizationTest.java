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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locks in that the classpath {@code app/src/main/resources/application.yaml} - the single source
 * of every module's shipped defaults, read directly through {@code io.avaje.config.Config} -
 * carries exactly the expected defaults of the {@code sit}, {@code spawn}, {@code tickle} and
 * {@code elytra} sections, key by key. Each module's own per-field configuration type is gone, so
 * the expected values are spelled out literally here instead of compared against one.
 *
 * <p>The navigator's destinations are not part of this file at all - they are guarded by
 * {@code net.onelitefeather.titan.app.feature.navigator.NavigatorDestinationTest} instead, as a
 * plain unit test of the {@code Destination} enum.
 *
 * <p>Loads the file as its own {@link Configuration} instance via
 * {@link Configuration.Builder#load(String)} - which reads a classpath resource, never the static
 * {@code io.avaje.config.Config} facade - so this test has no dependency on JVM-wide state and
 * stays Independent and Repeatable (F.I.R.S.T.).
 *
 * <p>This is the safety net that a shipped default cannot silently drift out from under whichever
 * module reads it.
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

    @DisplayName("config.watch: application.yaml ships file watching off by default")
    @Test
    void configWatchIsDisabledByDefault() {
        Configuration configuration = load();

        Assertions.assertFalse(
                configuration.getBool("config.watch.enabled"), "config.watch.enabled must ship off - the operator turns it on in their own file (see openspec/changes/config-reload-feature-flags/design.md, decision 1, and the lobby-module-config spec's \"Überwachung standardmäßig aus\" scenario)");
        Assertions.assertEquals(10, configuration.getInt("config.watch.delay"), "config.watch.delay");
        Assertions.assertEquals(10, configuration.getInt("config.watch.period"), "config.watch.period");
    }

    @DisplayName("elytra: application.yaml matches today's shipped defaults (30 / 40 ticks)")
    @Test
    void elytraMatchesDefaults() {
        Configuration configuration = load();

        Assertions.assertEquals(30, configuration.getInt("elytra.burnDurationTicks"), "elytra.burnDurationTicks");
        Assertions.assertEquals(40, configuration.getInt("elytra.cooldownTicks"), "elytra.cooldownTicks");
    }
}
