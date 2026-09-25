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
package net.onelitefeather.titan.app.feature.tickle;

/**
 * Pure parsing and validation for the {@code tickle} module's configuration value (see
 * {@code openspec/changes/avaje-config-facade/design.md}, decisions 3 and 4).
 *
 * <p>{@link #cooldownMillis(String)} is used directly as the mapping function of
 * {@code Config.getAs(COOLDOWN_KEY, TickleSettings::cooldownMillis)} in
 * {@link TickleModule#enable}:
 * it never touches {@code io.avaje.config.Config} itself, so it is unit-testable on its own, and
 * {@code getAs} wraps any exception it throws into an {@code IllegalStateException} that names
 * {@link #COOLDOWN_KEY} once and keeps this method's own exception as the cause (key in the
 * message, reason in the cause chain - verified against avaje-config 5.2's
 * {@code CoreConfiguration#getAs}).
 */
final class TickleSettings {

    /** The full key {@link #cooldownMillis(String)} reads and validates. */
    static final String COOLDOWN_KEY = "tickle.cooldownMillis";

    private TickleSettings() {
    }

    /**
     * Parses and validates the tickle cooldown.
     *
     * @param raw the configured cooldown in milliseconds, as text; must parse as a whole number
     *            that is not negative
     * @return {@code raw}, parsed
     * @throws NumberFormatException    if {@code raw} does not parse as a {@code long}
     * @throws IllegalArgumentException if the parsed value is negative
     */
    static long cooldownMillis(String raw) {
        long millis = Long.parseLong(raw);
        if (millis < 0) {
            throw new IllegalArgumentException("must not be negative, was " + millis);
        }
        return millis;
    }
}
