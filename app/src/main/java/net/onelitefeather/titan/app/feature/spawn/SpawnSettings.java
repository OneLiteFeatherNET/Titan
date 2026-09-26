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

    /**
     * The composite {@link RuntimeConfigFallback#resolve} key for {@link #currentHeightBounds()}:
     * {@link #MIN_HEIGHT_KEY} and {@link #MAX_HEIGHT_KEY} are cross-validated together (a valid
     * {@code minHeight} depends on the already-parsed {@code maxHeight}), so there is no single
     * "one key's own shipped default" to substitute for just one of them - an invalid group falls
     * the pair back to the shipped defaults together.
     */
    static final String HEIGHT_BOUNDS_KEY = "spawn.heightBounds";

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
     * {@link #MIN_HEIGHT_KEY}'s and {@link #MAX_HEIGHT_KEY}'s raw, not-yet-parsed live values,
     * read together since a valid {@code minHeightRaw} can only be checked against the
     * already-parsed {@code maxHeightRaw} - the raw pair {@link RuntimeConfigFallback#resolve}
     * caches per {@link #HEIGHT_BOUNDS_KEY} to detect a persistently invalid group without
     * re-parsing it.
     *
     * @param minHeightRaw the configured min height, as text
     * @param maxHeightRaw the configured max height, as text
     */
    record RawHeightBounds(String minHeightRaw, String maxHeightRaw) {
    }

    /**
     * Parses and cross-validates {@link #MIN_HEIGHT_KEY} and {@link #MAX_HEIGHT_KEY} together -
     * the {@link RuntimeConfigFallback#resolve} {@code parseAndValidate} function for
     * {@link #currentHeightBounds()}. Built on {@link #minHeight(int, int)} rather than
     * duplicating its rule.
     *
     * @param raw the raw live pair to parse and cross-validate
     * @return the parsed, valid height bounds
     * @throws NumberFormatException    if either raw value does not parse as an {@code int}; the
     *                                  message names whichever of {@link #MIN_HEIGHT_KEY} or
     *                                  {@link #MAX_HEIGHT_KEY} failed to parse
     * @throws IllegalArgumentException if {@code minHeightRaw} is not less than
     *                                  {@code maxHeightRaw}; see {@link #minHeight(int, int)}
     */
    static HeightSettings parseHeightBounds(RawHeightBounds raw) {
        int maxHeight = parseHeightComponent(MAX_HEIGHT_KEY, raw.maxHeightRaw());
        int parsedMinHeight = parseHeightComponent(MIN_HEIGHT_KEY, raw.minHeightRaw());
        return new HeightSettings(minHeight(parsedMinHeight, maxHeight), maxHeight);
    }

    private static int parseHeightComponent(String key, String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(key + ": " + e.getMessage());
        }
    }

    /**
     * @param shipped the shipped classpath defaults to read {@link #MIN_HEIGHT_KEY} and
     *                {@link #MAX_HEIGHT_KEY} from
     * @return the shipped height bounds - the {@link RuntimeConfigFallback#resolve}
     *         {@code shippedDefault} supplier for {@link #currentHeightBounds()}, evaluated only
     *         if the live pair is invalid
     */
    static HeightSettings shippedHeightBounds(Configuration shipped) {
        return new HeightSettings(shipped.getInt(MIN_HEIGHT_KEY), shipped.getInt(MAX_HEIGHT_KEY));
    }

    /**
     * Reads both height bounds live through the static facade, resolving an invalid or
     * persistently-invalid runtime value to its shipped default via the process-wide
     * {@link RuntimeConfigFallback}. Called directly by {@link SpawnBoundsListener} on every move
     * (see {@code openspec/changes/config-reload-feature-flags/design.md}, decision 1) - the
     * shipped defaults are read only if the live pair turns out invalid, never on every call.
     *
     * @return the current, valid height bounds
     */
    static HeightSettings currentHeightBounds() {
        RuntimeConfigFallback fallback = RuntimeConfigFallback.shared();
        RawHeightBounds raw = new RawHeightBounds(Config.get(MIN_HEIGHT_KEY), Config.get(MAX_HEIGHT_KEY));
        return fallback.resolve(HEIGHT_BOUNDS_KEY, raw, SpawnSettings::parseHeightBounds, () -> shippedHeightBounds(fallback.shippedDefaults()));
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
        return fallback.resolve(SIMULATION_DISTANCE_KEY, Config.get(SIMULATION_DISTANCE_KEY), SpawnSettings::simulationDistance, () -> fallback.shippedDefaults().getInt(SIMULATION_DISTANCE_KEY));
    }
}
