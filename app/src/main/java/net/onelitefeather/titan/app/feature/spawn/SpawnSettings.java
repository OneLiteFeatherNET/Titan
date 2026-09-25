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
package net.onelitefeather.titan.app.feature.spawn;

import net.onelitefeather.titan.common.config.ConfigException;

/**
 * Pure validation for the {@code spawn} section's values, kept apart from however those values
 * are read ({@link SpawnModule#enable}, via {@code io.avaje.config.Config} and
 * {@link net.onelitefeather.titan.common.config.ConfigValues}).
 *
 * <p>Every method takes plain values and either returns the validated one or throws
 * {@link ConfigException#invalid(String, String)} naming the value's full section key, e.g.
 * {@code spawn.minHeight}. None of these methods touch {@code io.avaje.config.Config} or a
 * server, so they are unit-testable on their own.
 *
 * <p>The keys themselves are declared here as constants, the one place this module's config
 * section is named (see {@code design.md}, decision 3), and reused by {@link SpawnModule#enable}
 * to read the raw values.
 */
final class SpawnSettings {

    static final String MIN_HEIGHT_KEY = "spawn.minHeight";
    static final String MAX_HEIGHT_KEY = "spawn.maxHeight";
    static final String SIMULATION_DISTANCE_KEY = "spawn.simulationDistance";

    private SpawnSettings() {
    }

    /**
     * @param minHeight the lowest {@code y} coordinate a player may fall to before being
     *                  teleported back to spawn
     * @param maxHeight the highest {@code y} coordinate a player may rise to before being
     *                  teleported back to spawn
     * @return {@code minHeight}, unchanged
     * @throws ConfigException if {@code minHeight} is not less than {@code maxHeight}; the
     *                         message names both {@code spawn.minHeight} and
     *                         {@code spawn.maxHeight}
     */
    static int minHeight(int minHeight, int maxHeight) {
        if (minHeight >= maxHeight) {
            throw ConfigException.invalid(MIN_HEIGHT_KEY, "must be less than " + MAX_HEIGHT_KEY + " (" + maxHeight + ")");
        }
        return minHeight;
    }

    /**
     * @param simulationDistance the simulation distance sent to a player on spawn
     * @return {@code simulationDistance}, unchanged
     * @throws ConfigException if {@code simulationDistance} is not positive
     */
    static int simulationDistance(int simulationDistance) {
        if (simulationDistance <= 0) {
            throw ConfigException.invalid(SIMULATION_DISTANCE_KEY, "must be greater than 0");
        }
        return simulationDistance;
    }
}
