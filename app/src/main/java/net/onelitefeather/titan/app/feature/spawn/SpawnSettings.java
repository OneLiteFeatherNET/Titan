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
 * were read (today {@link SpawnConfig}'s compact constructor, called by {@code SectionBinder}).
 *
 * <p>Every method takes plain values and either returns the validated one or throws
 * {@link ConfigException#invalid(String, String)} naming the value's full section key, e.g.
 * {@code spawn.minHeight}. None of these methods touch {@code io.avaje.config.Config},
 * {@code ConfigSections} or a server, so they are unit-testable on their own.
 */
final class SpawnSettings {

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
            throw ConfigException.invalid("spawn.minHeight", "must be less than spawn.maxHeight (" + maxHeight + ")");
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
            throw ConfigException.invalid("spawn.simulationDistance", "must be greater than 0");
        }
        return simulationDistance;
    }
}
