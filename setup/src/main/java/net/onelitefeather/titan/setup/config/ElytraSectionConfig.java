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

/**
 * Setup-local mirror of the lobby's {@code elytra} config section (id {@code "elytra"}).
 * <p>
 * The setup server does not depend on {@code app}, so it cannot reuse the real feature module's
 * config record; this record duplicates the two fields the {@code app} command edits. The lobby's
 * own {@code net.onelitefeather.titan.app.feature.elytra.ElytraConfig} was ported from Voyager
 * ({@code net.elytrarace.voyager.api.race.BoostConfig}): the boost is Vanilla's own firework
 * impulse, applied client-side, so there is no multiplier to tune, only how long one rocket burns
 * and how long a player waits before the next one.
 *
 * @param burnDurationTicks how many ticks one rocket boosts for
 * @param cooldownTicks     how many ticks after a boost starts before another may be used
 */
public record ElytraSectionConfig(int burnDurationTicks, int cooldownTicks) {

    /**
     * The defaults used when {@code app.json} has no {@code elytra} section yet, matching the
     * lobby's own {@code ElytraConfig.DEFAULTS}.
     */
    public static final ElytraSectionConfig DEFAULTS = new ElytraSectionConfig(30, 40);
}
