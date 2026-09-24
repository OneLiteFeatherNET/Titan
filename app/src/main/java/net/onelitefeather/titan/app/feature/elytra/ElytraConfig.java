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
 * The {@code elytra} module's own configuration section (see {@code lobby-module-config} spec).
 *
 * @param boostMultiplier scales the vanilla-equivalent firework boost applied by
 *                        {@link FireworkBoostTracker}. {@code 1.0} reproduces vanilla's own
 *                        boost exactly; a larger value boosts harder. Must be strictly positive.
 */
public record ElytraConfig(double boostMultiplier) {

    /**
     * Today's shipped value - the lobby has run with a 35x vanilla boost since before this module
     * existed, not the vanilla default of {@code 1.0}.
     */
    public static final ElytraConfig DEFAULTS = new ElytraConfig(35.0);

    public ElytraConfig {
        if (boostMultiplier <= 0) {
            throw ConfigException.invalid("boostMultiplier", "must be positive");
        }
    }
}
