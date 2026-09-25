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
 * <p>Ported from Voyager's {@code net.elytrarace.voyager.api.race.BoostConfig}: the boost itself is
 * Vanilla's own firework impulse, applied client-side once a rocket is attached to the flying
 * player, so there is no speed or multiplier to tune here - only how long one rocket burns for and
 * how long a player waits before the next one. See {@link FireworkBoostTracker} and
 * {@link FireworkRockets}.
 *
 * @param burnDurationTicks how many ticks one rocket boosts for; the rocket entity is removed
 *                          after exactly this many ticks. Must be strictly positive.
 * @param cooldownTicks     how many ticks after a boost <em>starts</em> before another may be
 *                          used. Measured from the burn's start, so it must be strictly longer
 *                          than {@code burnDurationTicks} - otherwise two rockets could burn on
 *                          the same player at once.
 */
public record ElytraConfig(int burnDurationTicks, int cooldownTicks) {

    /**
     * Voyager's own deterministic burn for Vanilla's strongest craftable rocket
     * ({@code 10 * flightDuration} with {@code flightDuration = 3}, i.e. 30 ticks - see
     * {@code BoostConfig.VANILLA_BURN_TICKS}) paired with the cooldown Voyager's reference map
     * ships (40 ticks, converted from the old tree's 2000 ms cooldown).
     */
    public static final ElytraConfig DEFAULTS = new ElytraConfig(30, 40);

    /**
     * Delegates to {@link ElytraSettings} so the validation rules live in exactly one place.
     *
     * @throws ConfigException naming {@link ElytraSettings#BURN_DURATION_TICKS_KEY} or
     *                         {@link ElytraSettings#COOLDOWN_TICKS_KEY} for an invalid value
     */
    public ElytraConfig {
        burnDurationTicks = ElytraSettings.burnDurationTicks(burnDurationTicks);
        cooldownTicks = ElytraSettings.cooldownTicks(cooldownTicks, burnDurationTicks);
    }
}
