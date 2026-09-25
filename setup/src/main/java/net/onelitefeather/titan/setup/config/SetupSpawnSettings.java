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

/**
 * Pure validation for {@code spawn.simulationDistance}, kept apart from however the value was
 * read (today {@link SetupSpawnConfig}'s compact constructor, fed by {@link
 * SetupSpawnConfig#read()}).
 *
 * <p>Mirrors the lobby's own rule for the same key (see {@code
 * net.onelitefeather.titan.app.feature.spawn.SpawnSettings#simulationDistance(int)}). This method
 * takes a plain value and either returns it unchanged or throws {@link
 * ConfigException#invalid(String, String)} naming the full key. It touches neither {@code
 * io.avaje.config.Config} nor a server, so it is unit-testable on its own (design.md, decisions 3
 * and 5).
 */
final class SetupSpawnSettings {

    private SetupSpawnSettings() {
    }

    /**
     * @param simulationDistance the simulation distance sent to a spawning player
     * @return {@code simulationDistance}, unchanged
     * @throws ConfigException if {@code simulationDistance} is not positive
     */
    static int simulationDistance(int simulationDistance) {
        if (simulationDistance <= 0) {
            throw ConfigException.invalid("spawn.simulationDistance", "must be greater than 0");
        }
        return simulationDistance;
    }
}
