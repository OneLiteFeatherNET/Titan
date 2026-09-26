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

import io.avaje.config.Config;
import io.avaje.config.Configuration;
import net.onelitefeather.titan.common.config.RuntimeConfigFallback;

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

    /**
     * Both live height bounds, read together since {@link #minHeight(int, int)} is a cross-field
     * check against the (possibly already-fallen-back) max height.
     *
     * @param minHeight the lowest {@code y} coordinate a player may fall to
     * @param maxHeight the highest {@code y} coordinate a player may rise to
     */
    record HeightSettings(int minHeight, int maxHeight) {
    }

    /**
     * The runtime counterpart of {@code spawn.maxHeight}'s parsing: a valid {@code raw} passes
     * through unchanged; an invalid one falls back to {@code shippedDefault}, after
     * {@code fallback} logs a deduplicated WARN naming {@link #MAX_HEIGHT_KEY}.
     *
     * @param raw            the configured max height, as read live from the facade
     * @param shippedDefault the shipped classpath default for {@link #MAX_HEIGHT_KEY}
     * @param fallback       deduplicates the WARN line for a repeated invalid value
     * @return {@code raw}, parsed, or {@code shippedDefault} if it does not parse
     */
    static int resolveMaxHeight(String raw, int shippedDefault, RuntimeConfigFallback fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback.fallback(MAX_HEIGHT_KEY, raw, e.getMessage(), shippedDefault);
        }
    }

    /**
     * The runtime counterpart of {@link #minHeight(int, int)}: a valid {@code raw} passes through
     * unchanged; an invalid one - not strictly less than {@code maxHeight} - falls back to
     * {@code shippedDefault}, after {@code fallback} logs a deduplicated WARN naming
     * {@link #MIN_HEIGHT_KEY}.
     *
     * @param raw            the configured min height, as read live from the facade
     * @param maxHeight      the already-resolved max height to compare against
     * @param shippedDefault the shipped classpath default for {@link #MIN_HEIGHT_KEY}
     * @param fallback       deduplicates the WARN line for a repeated invalid value
     * @return {@code raw}, parsed and validated, or {@code shippedDefault} if invalid
     */
    static int resolveMinHeight(String raw, int maxHeight, int shippedDefault, RuntimeConfigFallback fallback) {
        try {
            return minHeight(Integer.parseInt(raw), maxHeight);
        } catch (RuntimeException e) {
            return fallback.fallback(MIN_HEIGHT_KEY, raw, e.getMessage(), shippedDefault);
        }
    }

    /**
     * The runtime counterpart of {@link #simulationDistance(String)}: a valid {@code raw} passes
     * through unchanged; an invalid one falls back to {@code shippedDefault}, after
     * {@code fallback} logs a deduplicated WARN naming {@link #SIMULATION_DISTANCE_KEY}.
     *
     * @param raw            the configured simulation distance, as read live from the facade
     * @param shippedDefault the shipped classpath default for {@link #SIMULATION_DISTANCE_KEY}
     * @param fallback       deduplicates the WARN line for a repeated invalid value
     * @return {@code raw}, parsed and validated, or {@code shippedDefault} if invalid
     */
    static int resolveSimulationDistance(String raw, int shippedDefault, RuntimeConfigFallback fallback) {
        try {
            return simulationDistance(raw);
        } catch (RuntimeException e) {
            return fallback.fallback(SIMULATION_DISTANCE_KEY, raw, e.getMessage(), shippedDefault);
        }
    }

    /**
     * Reads both height bounds live through the static facade, resolving an invalid runtime value
     * to its shipped default via the process-wide {@link RuntimeConfigFallback}. Called directly
     * by {@link SpawnBoundsListener} on every move (see
     * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 1).
     *
     * @return the current, valid height bounds
     */
    static HeightSettings currentHeightBounds() {
        RuntimeConfigFallback fallback = RuntimeConfigFallback.shared();
        Configuration shipped = fallback.shippedDefaults();
        int maxHeight = resolveMaxHeight(Config.get(MAX_HEIGHT_KEY), shipped.getInt(MAX_HEIGHT_KEY), fallback);
        int minHeight = resolveMinHeight(Config.get(MIN_HEIGHT_KEY), maxHeight, shipped.getInt(MIN_HEIGHT_KEY), fallback);
        return new HeightSettings(minHeight, maxHeight);
    }

    /**
     * Reads the simulation distance live through the static facade, resolving an invalid runtime
     * value to the shipped default via the process-wide {@link RuntimeConfigFallback}. Called
     * directly by {@link SpawnJoinListener} on every join.
     *
     * @return the current, valid simulation distance
     */
    static int currentSimulationDistance() {
        RuntimeConfigFallback fallback = RuntimeConfigFallback.shared();
        return resolveSimulationDistance(Config.get(SIMULATION_DISTANCE_KEY), fallback.shippedDefaults().getInt(SIMULATION_DISTANCE_KEY), fallback);
    }
}
