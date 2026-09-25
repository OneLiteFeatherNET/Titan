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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link ConfigurationPropertyPlugin}: every method must read from the exact
 * {@link Configuration} instance it was built with, never touch the static {@code
 * io.avaje.config.Config} facade (see the class Javadoc for why that distinction matters). Every
 * {@link Configuration} here is built directly from a {@link Map} (design.md decision 1's spike
 * result), never from the real environment, so this stays hermetic (F.I.R.S.T. - Independent,
 * Repeatable).
 */
class ConfigurationPropertyPluginTest {

    @DisplayName("get(key) returns the value the backing Configuration holds")
    @Test
    void getReturnsTheValueFromTheBackingConfiguration() {
        Configuration configuration = Configuration.builder().putAll(Map.of("spawn.simulationDistance", "7")).build();
        ConfigurationPropertyPlugin plugin = new ConfigurationPropertyPlugin(configuration);

        Assertions.assertEquals(Optional.of("7"), plugin.get("spawn.simulationDistance"));
    }

    @DisplayName("get(key) is empty for a key the backing Configuration does not hold")
    @Test
    void getIsEmptyForAnAbsentKey() {
        Configuration configuration = Configuration.builder().putAll(Map.of()).build();
        ConfigurationPropertyPlugin plugin = new ConfigurationPropertyPlugin(configuration);

        Assertions.assertEquals(Optional.empty(), plugin.get("does.not.exist"));
    }

    @DisplayName("contains(key) is true only for a key the backing Configuration holds")
    @Test
    void containsReflectsTheBackingConfiguration() {
        Configuration configuration = Configuration.builder().putAll(Map.of("spawn.simulationDistance", "7")).build();
        ConfigurationPropertyPlugin plugin = new ConfigurationPropertyPlugin(configuration);

        Assertions.assertTrue(plugin.contains("spawn.simulationDistance"));
        Assertions.assertFalse(plugin.contains("does.not.exist"));
    }

    @DisplayName("equalTo(key, value) compares against the backing Configuration's current value")
    @Test
    void equalToComparesAgainstTheBackingConfigurationsValue() {
        Configuration configuration = Configuration.builder().putAll(Map.of("spawn.simulationDistance", "7")).build();
        ConfigurationPropertyPlugin plugin = new ConfigurationPropertyPlugin(configuration);

        Assertions.assertTrue(plugin.equalTo("spawn.simulationDistance", "7"));
        Assertions.assertFalse(plugin.equalTo("spawn.simulationDistance", "9"));
        Assertions.assertFalse(plugin.equalTo("does.not.exist", "7"));
    }
}
