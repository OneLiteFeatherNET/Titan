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
 * Pure parsing and validation for {@code spawn.simulationDistance}, kept apart from
 * {@link SetupSpawnConfig#read()}, which reads the raw value. Used as the mapping function of
 * {@code Config.getAs(KEY, SetupSpawnSettings::simulationDistance)}, which wraps any exception this
 * throws into an {@code IllegalStateException} naming the key. Touches neither
 * {@code io.avaje.config.Config} nor a server, so it is unit-testable on its own.
 */
final class SetupSpawnSettings {

    private SetupSpawnSettings() {
    }

    static int simulationDistance(String raw) {
        int simulationDistance = Integer.parseInt(raw);
        if (simulationDistance <= 0) {
            throw new IllegalArgumentException("must be greater than 0, was " + simulationDistance);
        }
        return simulationDistance;
    }
}
