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
package net.onelitefeather.titan.setup.config;

import net.onelitefeather.titan.common.config.ConfigException;

/**
 * Setup-local mirror of the lobby's {@code elytra} config section (id {@code "elytra"}).
 * <p>
 * The setup server does not depend on {@code app}, so it cannot reuse the real feature module's
 * config record; this record duplicates the two fields the {@code app} command edits, including
 * the validation in {@code net.onelitefeather.titan.app.feature.elytra.ElytraConfig}'s own compact
 * constructor - see that class' Javadoc for why the rule is what it is. Keeping the same rule here
 * is what lets {@link net.onelitefeather.titan.setup.config.SetupConfigEditor} reject a pair the
 * lobby would refuse to load, before writing it to {@code app.json} (see
 * {@code SetupConfigEditor#setElytraBurnDurationTicks}/{@code #setElytraCooldownTicks}), instead of
 * writing a value the lobby then fails to start with. The lobby's own
 * {@code net.onelitefeather.titan.app.feature.elytra.ElytraConfig} was ported from Voyager
 * ({@code net.elytrarace.voyager.api.race.BoostConfig}): the boost is Vanilla's own firework
 * impulse, applied client-side, so there is no multiplier to tune, only how long one rocket burns
 * and how long a player waits before the next one.
 *
 * @param burnDurationTicks how many ticks one rocket boosts for. Must be strictly positive.
 * @param cooldownTicks     how many ticks after a boost starts before another may be used. Must be
 *                          strictly longer than {@code burnDurationTicks}, for the same reason
 *                          {@code ElytraConfig} requires it: the cooldown is measured from the
 *                          burn's start, so a cooldown that is not longer than the burn would let
 *                          two rockets burn on the same player at once.
 */
public record ElytraSectionConfig(int burnDurationTicks, int cooldownTicks) {

    /**
     * The defaults used when {@code app.json} has no {@code elytra} section yet, matching the
     * lobby's own {@code ElytraConfig.DEFAULTS}.
     */
    public static final ElytraSectionConfig DEFAULTS = new ElytraSectionConfig(30, 40);

    public ElytraSectionConfig {
        if (burnDurationTicks <= 0) {
            throw ConfigException.invalid("burnDurationTicks", "must be positive");
        }
        if (cooldownTicks <= burnDurationTicks) {
            throw ConfigException.invalid("cooldownTicks", "must be longer than burnDurationTicks");
        }
    }
}
