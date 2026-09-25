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

/**
 * The setup server's only configuration value: the simulation distance sent to a spawning player
 * via {@link net.onelitefeather.titan.setup.listener.PlayerSpawnListener}.
 * <p>
 * {@link #read(Configuration)} reads {@code spawn.simulationDistance} directly with {@link
 * Configuration#getInt(String, int)}, not through {@link
 * net.onelitefeather.titan.common.config.ConfigSections}'s record binding. The lobby's own {@code
 * spawn} section (see {@code net.onelitefeather.titan.app.feature.spawn.SpawnConfig}) also carries
 * {@code minHeight}/{@code maxHeight}, fields this setup-local record does not declare; binding a
 * partial record over the same {@code application.yaml} the lobby reads would make {@code
 * ConfigSections} log a spurious "contains unknown keys" warning on every setup-server start. This
 * class avoids that by only ever reading the one key it needs.
 *
 * @param simulationDistance the simulation distance sent to a spawning player
 */
public record SetupSpawnConfig(int simulationDistance) {

    private static final String KEY = "spawn.simulationDistance";

    /** The default used when {@code application.yaml} sets no {@code spawn.simulationDistance}. */
    public static final SetupSpawnConfig DEFAULTS = new SetupSpawnConfig(2);

    /**
     * @param configuration the configuration to read {@value #KEY} from
     * @return {@value #KEY}, falling back to {@link #DEFAULTS} when unset
     */
    public static SetupSpawnConfig read(Configuration configuration) {
        return new SetupSpawnConfig(configuration.getInt(KEY, DEFAULTS.simulationDistance()));
    }
}
