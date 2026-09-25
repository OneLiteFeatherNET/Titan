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
package net.onelitefeather.titan.setup.config;

import io.avaje.config.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers {@link SetupSpawnConfig#read(Configuration)}, the only configuration value the setup
 * server reads (see {@code design.md} decision 5 of {@code standardized-config-profiles}). It
 * reads {@code spawn.simulationDistance} directly via {@link Configuration#getInt(String, int)}
 * rather than through {@link net.onelitefeather.titan.common.config.ConfigSections}'s record
 * binding: the lobby's own {@code spawn} section also carries {@code minHeight}/{@code maxHeight},
 * fields this setup-local record does not declare, and binding a partial record over a shared
 * {@code application.yaml} would make {@code ConfigSections} log a spurious "contains unknown
 * keys" warning on every setup-server start.
 */
class SetupSpawnConfigTest {

    @Test
    @DisplayName("Reads the configured simulation distance")
    void readsTheConfiguredSimulationDistance() {
        Configuration configuration = Configuration.builder().putAll(Map.of("spawn.simulationDistance", "4")).build();

        SetupSpawnConfig spawn = SetupSpawnConfig.read(configuration);

        assertEquals(4, spawn.simulationDistance());
    }

    @Test
    @DisplayName("Falls back to the default simulation distance (2) when unset")
    void fallsBackToTheDefaultWhenUnset() {
        Configuration configuration = Configuration.builder().build();

        SetupSpawnConfig spawn = SetupSpawnConfig.read(configuration);

        assertEquals(2, spawn.simulationDistance());
    }
}
