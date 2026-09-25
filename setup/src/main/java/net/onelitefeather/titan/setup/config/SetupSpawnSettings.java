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

/**
 * Pure parsing and validation for {@code spawn.simulationDistance}, kept apart from however the
 * value was read (today {@link SetupSpawnConfig#read()}).
 *
 * <p>Mirrors the lobby's own rule for the same key (see
 * {@code net.onelitefeather.titan.app.feature.spawn.SpawnSettings#simulationDistance(String)}).
 * Used directly as the mapping function of
 * {@code Config.getAs(KEY, SetupSpawnSettings::simulationDistance)} - {@code getAs} wraps any
 * exception it throws into an {@code IllegalStateException} naming the key once, keeping this
 * method's own exception as the cause. It touches neither {@code io.avaje.config.Config} nor a
 * server, so it is unit-testable on its own (design.md, decisions 3, 4 and 5).
 */
final class SetupSpawnSettings {

    private SetupSpawnSettings() {
    }

    /**
     * @param raw the configured simulation distance, as text; must parse as a strictly positive int
     * @return {@code raw}, parsed
     * @throws NumberFormatException    if {@code raw} does not parse as an {@code int}
     * @throws IllegalArgumentException if the parsed value is not positive
     */
    static int simulationDistance(String raw) {
        int simulationDistance = Integer.parseInt(raw);
        if (simulationDistance <= 0) {
            throw new IllegalArgumentException("must be greater than 0, was " + simulationDistance);
        }
        return simulationDistance;
    }
}
