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

import io.avaje.config.Config;

/**
 * The setup server's only configuration value: the simulation distance sent to a spawning player
 * via {@link net.onelitefeather.titan.setup.listener.PlayerSpawnListener}. {@link #read()} reads
 * and validates {@code spawn.simulationDistance} from the {@code io.avaje.config.Config} facade;
 * {@code application.yaml} ships {@code 2} as the default.
 *
 * @param simulationDistance the already-validated simulation distance sent to a spawning player
 */
public record SetupSpawnConfig(int simulationDistance) {

    private static final String KEY = "spawn.simulationDistance";

    /**
     * @return {@value #KEY}, read and validated from the {@code io.avaje.config.Config} facade
     * @throws IllegalStateException if the key is missing, not a whole number, or not positive
     */
    public static SetupSpawnConfig read() {
        return new SetupSpawnConfig(Config.getAs(KEY, SetupSpawnSettings::simulationDistance));
    }
}
