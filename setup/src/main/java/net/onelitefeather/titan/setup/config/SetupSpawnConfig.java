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

import net.onelitefeather.titan.common.config.ConfigException;
import net.onelitefeather.titan.common.config.ConfigValues;

/**
 * The setup server's only configuration value: the simulation distance sent to a spawning player
 * via {@link net.onelitefeather.titan.setup.listener.PlayerSpawnListener}.
 * <p>
 * {@link #read()} reads {@code spawn.simulationDistance} at the edge, directly from the static
 * {@code io.avaje.config.Config} facade via {@link ConfigValues#intValue(String)} (design.md,
 * decisions 3 and 4). There is no default in code;
 * {@code setup/src/main/resources/application.yaml}
 * ships {@code spawn.simulationDistance: 2} as the shipped default, the same way the lobby's own
 * {@code spawn} section ships its defaults.
 *
 * @param simulationDistance the simulation distance sent to a spawning player
 */
public record SetupSpawnConfig(int simulationDistance) {

    private static final String KEY = "spawn.simulationDistance";

    /**
     * @throws ConfigException if {@code simulationDistance} is not positive; see
     *                         {@link SetupSpawnSettings#simulationDistance(int)}
     */
    public SetupSpawnConfig {
        simulationDistance = SetupSpawnSettings.simulationDistance(simulationDistance);
    }

    /**
     * @return {@value #KEY}, read from the {@code io.avaje.config.Config} facade
     * @throws ConfigException if the key is missing, not a whole number, or not positive
     */
    public static SetupSpawnConfig read() {
        return new SetupSpawnConfig(ConfigValues.intValue(KEY));
    }
}
