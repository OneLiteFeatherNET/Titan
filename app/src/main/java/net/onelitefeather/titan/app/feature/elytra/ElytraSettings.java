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
}
