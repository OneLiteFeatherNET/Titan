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
 * Setup-local mirror of the lobby's {@code spawn} config section (id {@code "spawn"}).
 * <p>
 * The setup server does not depend on {@code app}, so it cannot reuse the real feature module's
 * config record; this record duplicates the handful of fields the setup server actually reads
 * (the simulation distance sent to a spawning player) or must not disturb when it edits another
 * field of {@code app.json} (the height bounds, edited only by the lobby itself).
 *
 * @param minHeight          the minimum height before the lobby teleports a player back
 * @param maxHeight          the maximum height before the lobby teleports a player back
 * @param simulationDistance the simulation distance sent to a spawning player
 */
public record SpawnSectionConfig(int minHeight, int maxHeight, int simulationDistance) {

    /**
     * The defaults used when {@code app.json} has no {@code spawn} section yet, matching the
     * lobby's own defaults (see {@code InternalAppConfig}).
     */
    public static final SpawnSectionConfig DEFAULTS = new SpawnSectionConfig(-64, 310, 2);
}
