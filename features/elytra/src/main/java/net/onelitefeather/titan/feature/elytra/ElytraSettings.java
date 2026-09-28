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
package net.onelitefeather.titan.feature.elytra;

/**
 * Pure parsing and validation for the {@code elytra} module's configuration values. Both checks
 * run once, at startup ({@link ElytraModule#start}); the firework item's use handler reads the
 * same keys again on every boost, live and unvalidated, so a runtime change is never re-validated.
 */
final class ElytraSettings {

    static final String BURN_DURATION_TICKS_KEY = "elytra.burnDurationTicks";

    static final String COOLDOWN_TICKS_KEY = "elytra.cooldownTicks";

    private ElytraSettings() {
    }

    /** @throws IllegalArgumentException if the parsed value is not positive */
    static int burnDurationTicks(String raw) {
        int burnDurationTicks = Integer.parseInt(raw);
        if (burnDurationTicks <= 0) {
            throw new IllegalArgumentException("must be positive, was " + burnDurationTicks);
        }
        return burnDurationTicks;
    }

    /**
     * @throws IllegalArgumentException if {@code cooldownTicks} is not strictly longer than
     *                                  {@code burnDurationTicks} - otherwise two rockets could burn
     *                                  on the same player at once
     */
    static int cooldownTicks(int cooldownTicks, int burnDurationTicks) {
        if (cooldownTicks <= burnDurationTicks) {
            throw new IllegalArgumentException(COOLDOWN_TICKS_KEY + " (" + cooldownTicks + ") must be longer than " + BURN_DURATION_TICKS_KEY + " (" + burnDurationTicks + ")");
        }
        return cooldownTicks;
    }

}
