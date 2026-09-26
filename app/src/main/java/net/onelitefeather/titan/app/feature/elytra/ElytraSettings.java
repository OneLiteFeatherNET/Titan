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
package net.onelitefeather.titan.app.feature.elytra;

import io.avaje.config.Config;
import io.avaje.config.Configuration;
import net.onelitefeather.titan.common.config.RuntimeConfigFallback;

/**
 * Pure parsing and validation for the {@code elytra} module's configuration values (see
 * {@code openspec/changes/avaje-config-facade/design.md}, decisions 3 and 4).
 *
 * <p>{@link #burnDurationTicks(String)} is a single-value check, used directly as the mapping
 * function of {@code Config.getAs(BURN_DURATION_TICKS_KEY, ElytraSettings::burnDurationTicks)} in
 * {@link ElytraModule#enable} - {@code getAs} wraps any exception it throws into an
 * {@code IllegalStateException} naming the key once, keeping this method's own exception as the
 * cause. {@link #cooldownTicks(int, int)} is a cross-field check - it needs the already-validated
 * burn duration - so {@link ElytraModule#enable} calls it itself, after reading both values; its
 * own
 * message therefore names both full keys.
 */
final class ElytraSettings {

    /** The full key {@link #burnDurationTicks(String)} reads and validates. */
    static final String BURN_DURATION_TICKS_KEY = "elytra.burnDurationTicks";

    /**
     * The full key {@link #cooldownTicks(int, int)}'s {@code cooldownTicks} parameter is read from.
     */
    static final String COOLDOWN_TICKS_KEY = "elytra.cooldownTicks";

    private ElytraSettings() {
    }

    /**
     * Parses and validates how many ticks one rocket boosts for.
     *
     * @param raw the configured burn duration, as text; must parse as a strictly positive int
     * @return {@code raw}, parsed
     * @throws NumberFormatException    if {@code raw} does not parse as an {@code int}
     * @throws IllegalArgumentException if the parsed value is not positive
     */
    static int burnDurationTicks(String raw) {
        int burnDurationTicks = Integer.parseInt(raw);
        if (burnDurationTicks <= 0) {
            throw new IllegalArgumentException("must be positive, was " + burnDurationTicks);
        }
        return burnDurationTicks;
    }

    /**
     * Validates how many ticks after a boost starts before another may be used. Measured from the
     * burn's start, so it must be strictly longer than {@code burnDurationTicks} - otherwise two
     * rockets could burn on the same player at once. Not read through {@code Config.getAs}'s
     * mapping function (unlike {@link #burnDurationTicks(String)}): it needs the already-validated
     * burn duration, so it self-names both full keys in its message.
     *
     * @param cooldownTicks     the configured cooldown; must be strictly longer than
     *                          {@code burnDurationTicks}
     * @param burnDurationTicks the already-validated burn duration to compare against
     * @return {@code cooldownTicks} unchanged
     * @throws IllegalArgumentException if it is not longer than {@code burnDurationTicks}; the
     *                                  message names both {@link #COOLDOWN_TICKS_KEY} and
     *                                  {@link #BURN_DURATION_TICKS_KEY}
     */
    static int cooldownTicks(int cooldownTicks, int burnDurationTicks) {
        if (cooldownTicks <= burnDurationTicks) {
            throw new IllegalArgumentException(COOLDOWN_TICKS_KEY + " (" + cooldownTicks + ") must be longer than " + BURN_DURATION_TICKS_KEY + " (" + burnDurationTicks + ")");
        }
        return cooldownTicks;
    }

    /**
     * Both live boost settings, read together since {@link #cooldownTicks(int, int)} is a
     * cross-field check against the (possibly already-fallen-back) burn duration.
     *
     * @param burnDurationTicks how many ticks one rocket boosts for
     * @param cooldownTicks     how many ticks after a boost starts before another may be used
     */
    record BoostSettings(int burnDurationTicks, int cooldownTicks) {
    }

    /**
     * The runtime counterpart of {@link #burnDurationTicks(String)}: a valid {@code raw} passes
     * through unchanged; an invalid one falls back to {@code shippedDefault}, after
     * {@code fallback} logs a deduplicated WARN naming {@link #BURN_DURATION_TICKS_KEY}.
     *
     * @param raw            the configured burn duration, as read live from the facade
     * @param shippedDefault the shipped classpath default for {@link #BURN_DURATION_TICKS_KEY}
     * @param fallback       deduplicates the WARN line for a repeated invalid value
     * @return {@code raw}, parsed and validated, or {@code shippedDefault} if invalid
     */
    static int resolveBurnDurationTicks(String raw, int shippedDefault, RuntimeConfigFallback fallback) {
        try {
            return burnDurationTicks(raw);
        } catch (RuntimeException e) {
            return fallback.fallback(BURN_DURATION_TICKS_KEY, raw, e.getMessage(), shippedDefault);
        }
    }

    /**
     * The runtime counterpart of {@link #cooldownTicks(int, int)}: a valid {@code raw} passes
     * through unchanged; an invalid one - not strictly longer than {@code burnDurationTicks} - falls
     * back to {@code shippedDefault}, after {@code fallback} logs a deduplicated WARN naming
     * {@link #COOLDOWN_TICKS_KEY}.
     *
     * @param raw               the configured cooldown, as read live from the facade
     * @param burnDurationTicks the already-resolved burn duration to compare against
     * @param shippedDefault    the shipped classpath default for {@link #COOLDOWN_TICKS_KEY}
     * @param fallback          deduplicates the WARN line for a repeated invalid value
     * @return {@code raw}, parsed and validated, or {@code shippedDefault} if invalid
     */
    static int resolveCooldownTicks(String raw, int burnDurationTicks, int shippedDefault, RuntimeConfigFallback fallback) {
        try {
            return cooldownTicks(Integer.parseInt(raw), burnDurationTicks);
        } catch (RuntimeException e) {
            return fallback.fallback(COOLDOWN_TICKS_KEY, raw, e.getMessage(), shippedDefault);
        }
    }

    /**
     * Reads both boost settings live through the static facade, resolving an invalid runtime
     * value to its shipped default via the process-wide {@link RuntimeConfigFallback}. Called
     * directly by the {@code titan:firework} item's use handler on every boost (see
     * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 1), so a changed
     * value applies to the very next boost without a module restart.
     *
     * @return the current, valid boost settings
     */
    static BoostSettings current() {
        RuntimeConfigFallback fallback = RuntimeConfigFallback.shared();
        Configuration shipped = fallback.shippedDefaults();
        int burnDurationTicks = resolveBurnDurationTicks(Config.get(BURN_DURATION_TICKS_KEY), shipped.getAs(BURN_DURATION_TICKS_KEY, Integer::parseInt), fallback);
        int cooldownTicks = resolveCooldownTicks(Config.get(COOLDOWN_TICKS_KEY), burnDurationTicks, shipped.getAs(COOLDOWN_TICKS_KEY, Integer::parseInt), fallback);
        return new BoostSettings(burnDurationTicks, cooldownTicks);
    }
}
