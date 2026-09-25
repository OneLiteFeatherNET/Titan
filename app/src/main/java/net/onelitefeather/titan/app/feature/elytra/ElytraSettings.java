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

import net.onelitefeather.titan.common.config.ConfigException;

/**
 * Pure validation for the {@code elytra} module's configuration values (see
 * {@code openspec/changes/avaje-config-facade/design.md}, decision 3).
 *
 * <p>Takes plain values and either returns a validated result or throws
 * {@link ConfigException#invalid(String, String)} naming the full configuration key - it never
 * touches {@code io.avaje.config.Config} or a server, so it is unit-testable on its own.
 * {@link ElytraConfig}'s compact constructor delegates here so the same rules are not duplicated.
 */
final class ElytraSettings {

    /** The full key for {@link #burnDurationTicks(int)}'s parameter. */
    static final String BURN_DURATION_TICKS_KEY = "elytra.burnDurationTicks";

    /** The full key for {@link #cooldownTicks(int, int)}'s {@code cooldownTicks} parameter. */
    static final String COOLDOWN_TICKS_KEY = "elytra.cooldownTicks";

    private ElytraSettings() {
    }

    /**
     * Validates how many ticks one rocket boosts for.
     *
     * @param burnDurationTicks the configured burn duration; must be strictly positive
     * @return {@code burnDurationTicks} unchanged
     * @throws ConfigException naming {@link #BURN_DURATION_TICKS_KEY} if it is not positive
     */
    static int burnDurationTicks(int burnDurationTicks) {
        if (burnDurationTicks <= 0) {
            throw ConfigException.invalid(BURN_DURATION_TICKS_KEY, "must be positive");
        }
        return burnDurationTicks;
    }

    /**
     * Validates how many ticks after a boost starts before another may be used. Measured from the
     * burn's start, so it must be strictly longer than {@code burnDurationTicks} - otherwise two
     * rockets could burn on the same player at once.
     *
     * @param cooldownTicks     the configured cooldown; must be strictly longer than
     *                          {@code burnDurationTicks}
     * @param burnDurationTicks the already-validated burn duration to compare against
     * @return {@code cooldownTicks} unchanged
     * @throws ConfigException naming {@link #COOLDOWN_TICKS_KEY} if it is not longer than
     *                         {@code burnDurationTicks}
     */
    static int cooldownTicks(int cooldownTicks, int burnDurationTicks) {
        if (cooldownTicks <= burnDurationTicks) {
            throw ConfigException.invalid(COOLDOWN_TICKS_KEY, "must be longer than burnDurationTicks");
        }
        return cooldownTicks;
    }
}
