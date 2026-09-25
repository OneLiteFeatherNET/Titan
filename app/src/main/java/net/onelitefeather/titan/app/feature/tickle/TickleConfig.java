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

import net.onelitefeather.titan.common.config.ConfigException;

/**
 * The {@code tickle} module's own {@code app.json} section.
 *
 * @param cooldownMillis how long, in milliseconds, an attacking player must wait before they can
 *                       tickle again; must not be negative
 */
public record TickleConfig(long cooldownMillis) {

    /** This section's defaults, used when {@code app.json} has no {@code tickle} section at all. */
    public static final TickleConfig DEFAULTS = new TickleConfig(4000);

    /**
     * @throws ConfigException if {@code cooldownMillis} is negative
     */
    public TickleConfig {
        if (cooldownMillis < 0) {
            throw ConfigException.invalid("cooldownMillis", "must not be negative");
        }
    }
}
