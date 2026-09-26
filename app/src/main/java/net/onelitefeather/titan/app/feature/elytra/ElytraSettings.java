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

    /**
     * The composite {@link RuntimeConfigFallback#resolve} key for {@link #current()}:
     * {@link #BURN_DURATION_TICKS_KEY} and {@link #COOLDOWN_TICKS_KEY} are cross-validated
     * together (a valid {@code cooldownTicks} depends on the already-parsed
     * {@code burnDurationTicks}), so an invalid group falls the pair back to the shipped
     * defaults together.
     */
    static final String BOOST_SETTINGS_KEY = "elytra.boostSettings";

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
     * {@link #BURN_DURATION_TICKS_KEY}'s and {@link #COOLDOWN_TICKS_KEY}'s raw, not-yet-parsed
     * live values, read together since a valid {@code cooldownTicksRaw} can only be checked
     * against the already-parsed {@code burnDurationTicksRaw} - the raw pair
     * {@link RuntimeConfigFallback#resolve} caches per {@link #BOOST_SETTINGS_KEY} to detect a
     * persistently invalid group without re-parsing it.
     *
     * @param burnDurationTicksRaw the configured burn duration, as text
     * @param cooldownTicksRaw     the configured cooldown, as text
     */
    record RawBoostSettings(String burnDurationTicksRaw, String cooldownTicksRaw) {
    }

    /**
     * Parses and cross-validates {@link #BURN_DURATION_TICKS_KEY} and {@link #COOLDOWN_TICKS_KEY}
     * together - the {@link RuntimeConfigFallback#resolve} {@code parseAndValidate} function for
     * {@link #current()}. Built on {@link #burnDurationTicks(String)} and
     * {@link #cooldownTicks(int, int)} rather than duplicating their rules.
     *
     * @param raw the raw live pair to parse and cross-validate
     * @return the parsed, valid boost settings
     * @throws NumberFormatException    if either raw value does not parse as an {@code int}
     * @throws IllegalArgumentException if either value fails its own check, or
     *                                  {@code cooldownTicksRaw} is not strictly longer than
     *                                  {@code burnDurationTicksRaw}
     */
    static BoostSettings parseBoostSettings(RawBoostSettings raw) {
        int burnDurationTicks = burnDurationTicks(raw.burnDurationTicksRaw());
        int cooldownTicks = cooldownTicks(Integer.parseInt(raw.cooldownTicksRaw()), burnDurationTicks);
        return new BoostSettings(burnDurationTicks, cooldownTicks);
    }

    /**
     * @param shipped the shipped classpath defaults to read {@link #BURN_DURATION_TICKS_KEY} and
     *                {@link #COOLDOWN_TICKS_KEY} from
     * @return the shipped boost settings - the {@link RuntimeConfigFallback#resolve}
     *         {@code shippedDefault} supplier for {@link #current()}, evaluated only if the live
     *         pair is invalid
     */
    static BoostSettings shippedBoostSettings(Configuration shipped) {
        int burnDurationTicks = shipped.getAs(BURN_DURATION_TICKS_KEY, Integer::parseInt);
        int cooldownTicks = shipped.getAs(COOLDOWN_TICKS_KEY, Integer::parseInt);
        return new BoostSettings(burnDurationTicks, cooldownTicks);
    }

    /**
     * Reads both boost settings live through the static facade, resolving an invalid or
     * persistently-invalid runtime value to its shipped default via the process-wide
     * {@link RuntimeConfigFallback}. Called directly by the {@code titan:firework} item's use
     * handler on every boost (see {@code openspec/changes/config-reload-feature-flags/design.md},
     * decision 1), so a changed value applies to the very next boost without a module restart -
     * the shipped defaults are read only if the live pair turns out invalid, never on every call.
     *
     * @return the current, valid boost settings
     */
    static BoostSettings current() {
        RuntimeConfigFallback fallback = RuntimeConfigFallback.shared();
        RawBoostSettings raw = new RawBoostSettings(Config.get(BURN_DURATION_TICKS_KEY), Config.get(COOLDOWN_TICKS_KEY));
        return fallback.resolve(BOOST_SETTINGS_KEY, raw, ElytraSettings::parseBoostSettings, () -> shippedBoostSettings(fallback.shippedDefaults()));
    }
}
