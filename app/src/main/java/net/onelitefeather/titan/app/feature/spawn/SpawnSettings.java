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

/**
 * Pure parsing and validation for the {@code spawn} section's values, kept apart from however
 * those values are read ({@link SpawnModule#enable}, via {@code io.avaje.config.Config}).
 *
 * <p>{@link #simulationDistance(String)} is a single-value check, used directly as the mapping
 * function of {@code Config.getAs(SIMULATION_DISTANCE_KEY, SpawnSettings::simulationDistance)} -
 * {@code getAs} wraps any exception it throws into an {@code IllegalStateException} naming the key
 * once, keeping this method's own exception as the cause. {@link #minHeight(int, int)} is a
 * cross-field check - it needs both already-parsed heights - so {@link SpawnModule#enable} calls
 * it itself, after reading both values; its own message therefore names both full keys.
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
     * Not read through {@code Config.getAs}'s mapping function (unlike
     * {@link #simulationDistance(String)}): it needs both already-parsed heights, so it self-names
     * both full keys in its message.
     *
     * @param minHeight the lowest {@code y} coordinate a player may fall to before being
     *                  teleported back to spawn
     * @param maxHeight the highest {@code y} coordinate a player may rise to before being
     *                  teleported back to spawn
     * @return {@code minHeight}, unchanged
     * @throws IllegalArgumentException if {@code minHeight} is not less than {@code maxHeight}; the
     *                                  message names both {@link #MIN_HEIGHT_KEY} and
     *                                  {@link #MAX_HEIGHT_KEY}
     */
    static int minHeight(int minHeight, int maxHeight) {
        if (minHeight >= maxHeight) {
            throw new IllegalArgumentException(MIN_HEIGHT_KEY + " (" + minHeight + ") must be less than " + MAX_HEIGHT_KEY + " (" + maxHeight + ")");
        }
        return minHeight;
    }

    /**
     * Parses and validates the simulation distance sent to a player on spawn.
     *
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
