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
package net.onelitefeather.titan.app.feature.spawn;

import net.onelitefeather.titan.common.config.ConfigException;

/**
 * The {@code spawn} module's own configuration section, read via
 * {@code ModuleContext.config(SpawnConfig.class, SpawnConfig.DEFAULTS)}.
 *
 * @param minHeight          the lowest {@code y} coordinate a player may fall to before being
 *                           teleported back to spawn
 * @param maxHeight          the highest {@code y} coordinate a player may rise to before being
 *                           teleported back to spawn
 * @param simulationDistance the simulation distance sent to a player on spawn, via
 *                           {@link net.minestom.server.network.packet.server.play.UpdateSimulationDistancePacket}
 */
public record SpawnConfig(int minHeight, int maxHeight, int simulationDistance) {

    /** The section's defaults, per the {@code lobby-module-config} spec. */
    public static final SpawnConfig DEFAULTS = new SpawnConfig(-64, 310, 2);

    /**
     * @throws ConfigException if {@code minHeight} is not less than {@code maxHeight}, or if
     *                         {@code simulationDistance} is not positive
     */
    public SpawnConfig {
        if (minHeight >= maxHeight) {
            throw ConfigException.invalid("minHeight", "must be less than maxHeight (" + maxHeight + ")");
        }
        if (simulationDistance <= 0) {
            throw ConfigException.invalid("simulationDistance", "must be greater than 0");
        }
    }
}
