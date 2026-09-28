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

import io.avaje.config.Config;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.packet.server.play.UpdateSimulationDistancePacket;
import net.onelitefeather.titan.core.module.item.LobbyItems;

/**
 * Reacts to a player spawning in the lobby: sends the configured simulation distance, teleports
 * the player to the lobby spawn, and equips them with the platform-wide standard loadout via
 * {@link LobbyItems#equip(Player)}. Reads {@link SpawnSettings#SIMULATION_DISTANCE_KEY} live on
 * every join rather than once at construction, so a changed value applies immediately without a
 * module restart.
 */
final class SpawnJoinListener implements Consumer<PlayerSpawnEvent> {

    private final Supplier<Pos> spawnPosition;
    private final LobbyItems lobbyItems;

    SpawnJoinListener(Supplier<Pos> spawnPosition, LobbyItems lobbyItems) {
        this.spawnPosition = spawnPosition;
        this.lobbyItems = lobbyItems;
    }

    @Override
    public void accept(PlayerSpawnEvent event) {
        Player player = event.getPlayer();
        player.sendPacket(new UpdateSimulationDistancePacket(Config.getInt(SpawnSettings.SIMULATION_DISTANCE_KEY)));
        Optional.ofNullable(this.spawnPosition.get()).ifPresent(player::teleport);
        this.lobbyItems.equip(player);
    }
}
