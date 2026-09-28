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
package net.onelitefeather.titan.feature.spawn;

/**
 * Pure parsing and validation for the {@code spawn} section's values, kept apart from however
 * those values are read ({@link SpawnModule#start()}).
 *
 * <p>{@link #minHeight(int, int)} is a cross-field check, called directly after both heights are
 * parsed rather than through {@code Config.getAs}'s mapping function.
 */
final class SpawnSettings {

    static final String MIN_HEIGHT_KEY = "spawn.minHeight";
    static final String MAX_HEIGHT_KEY = "spawn.maxHeight";
    static final String SIMULATION_DISTANCE_KEY = "spawn.simulationDistance";

    private SpawnSettings() {
    }

    static int minHeight(int minHeight, int maxHeight) {
        if (minHeight >= maxHeight) {
            throw new IllegalArgumentException(MIN_HEIGHT_KEY + " (" + minHeight + ") must be less than " + MAX_HEIGHT_KEY + " (" + maxHeight + ")");
        }
        return minHeight;
    }

    static int simulationDistance(String raw) {
        int simulationDistance = Integer.parseInt(raw);
        if (simulationDistance <= 0) {
            throw new IllegalArgumentException("must be greater than 0, was " + simulationDistance);
        }
        return simulationDistance;
    }

}
