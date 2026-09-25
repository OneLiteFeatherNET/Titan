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
package net.onelitefeather.titan.app.feature.example;

import net.onelitefeather.titan.common.config.ConfigException;

/**
 * The {@code example} module's own configuration section - a template for a new feature's own
 * config record, see {@code docs/lobby-modules.md}.
 *
 * <p>Validated in the compact constructor, exactly like every other feature's config record (see
 * {@code SitConfig}, {@code TickleConfig}): a config record only knows its own field and the reason
 * a value is rejected, never which section it was loaded from -
 * {@link net.onelitefeather.titan.common.config.ConfigSections} fills that in before the exception
 * reaches {@link net.onelitefeather.titan.app.module.ModuleContext#config}'s caller.
 *
 * @param greeting       the message sent to a greeted player; must contain exactly one {@code %s}
 *                       placeholder for the player's name
 * @param cooldownMillis how long, in milliseconds, a player must wait before being greeted again;
 *                       must not be negative
 */
public record ExampleConfig(String greeting, long cooldownMillis) {

    /**
     * This section's defaults, used when {@code app.json} has no {@code example} section at all.
     */
    public static final ExampleConfig DEFAULTS = new ExampleConfig("Welcome to the lobby, %s!", 5000);

    /**
     * @throws ConfigException if {@code greeting} is blank, does not contain a {@code %s}
     *                         placeholder, or if {@code cooldownMillis} is negative
     */
    public ExampleConfig {
        if (greeting == null || greeting.isBlank()) {
            throw ConfigException.invalid("greeting", "must not be blank");
        }
        if (!greeting.contains("%s")) {
            throw ConfigException.invalid("greeting", "must contain a '%s' placeholder for the player's name");
        }
        if (cooldownMillis < 0) {
            throw ConfigException.invalid("cooldownMillis", "must not be negative");
        }
    }
}
