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

import java.time.Duration;
import net.onelitefeather.titan.common.config.ConfigException;

/**
 * Pure validation for the {@code tickle} module's configuration values (see
 * {@code openspec/changes/avaje-config-facade/design.md}, decision 3).
 *
 * <p>Takes plain values and either returns a validated result or throws
 * {@link ConfigException#invalid(String, String)} naming the full configuration key - it never
 * touches {@code io.avaje.config.Config} or a server, so it is unit-testable on its own.
 * {@link TickleConfig}'s compact constructor delegates here so the same rule is not duplicated.
 */
final class TickleSettings {

    /** The full key for {@link #cooldown(long)}'s {@code millis} parameter. */
    static final String COOLDOWN_KEY = "tickle.cooldownMillis";

    private TickleSettings() {
    }

    /**
     * Validates the tickle cooldown.
     *
     * @param millis how long, in milliseconds, an attacking player must wait before they can
     *               tickle again; must not be negative
     * @return {@code millis} as a {@link Duration}
     * @throws ConfigException naming {@link #COOLDOWN_KEY} if {@code millis} is negative
     */
    static Duration cooldown(long millis) {
        if (millis < 0) {
            throw ConfigException.invalid(COOLDOWN_KEY, "must not be negative");
        }
        return Duration.ofMillis(millis);
    }
}
