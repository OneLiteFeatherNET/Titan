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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.packet.server.CachedPacket;
import net.minestom.server.network.packet.server.play.UpdateSimulationDistancePacket;
import net.onelitefeather.titan.app.module.item.ModuleItems;

/**
 * Reacts to a player spawning in the lobby, mirroring the lobby's former
 * {@code PlayerSpawnListener}: sends the configured simulation distance, teleports the player to
 * the lobby spawn, and equips them with the platform-wide standard loadout via
 * {@link ModuleItems#equip(Player)}.
 */
final class SpawnJoinListener implements Consumer<PlayerSpawnEvent> {

    private final CachedPacket simulationDistancePacket;
    private final Supplier<Pos> spawnPosition;
    private final ModuleItems items;

    SpawnJoinListener(int simulationDistance, Supplier<Pos> spawnPosition, ModuleItems items) {
        this.simulationDistancePacket = new CachedPacket(new UpdateSimulationDistancePacket(simulationDistance));
        this.spawnPosition = spawnPosition;
        this.items = items;
    }

    @Override
    public void accept(PlayerSpawnEvent event) {
        Player player = event.getPlayer();
        player.sendPacket(this.simulationDistancePacket);
        Optional.ofNullable(this.spawnPosition.get()).ifPresent(player::teleport);
        this.items.equip(player);
    }
}
