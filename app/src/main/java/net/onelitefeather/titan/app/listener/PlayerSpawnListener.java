/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.packet.server.CachedPacket;
import net.minestom.server.network.packet.server.play.UpdateSimulationDistancePacket;
import net.onelitefeather.titan.app.helper.NavigationHelper;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.map.LobbyMap;

import java.util.Optional;
import java.util.function.Consumer;

public final class PlayerSpawnListener implements Consumer<PlayerSpawnEvent> {

    private final CachedPacket simulatedDistancePacket;
    private final LobbyMap lobbyMap;
    private final NavigationHelper navigationHelper;

    public PlayerSpawnListener(AppConfig appConfig, LobbyMap lobbyMap, NavigationHelper navigationHelper) {
        simulatedDistancePacket = new CachedPacket(new UpdateSimulationDistancePacket(appConfig.simulationDistance()));
        this.lobbyMap = lobbyMap;
        this.navigationHelper = navigationHelper;
    }

    @Override
    public void accept(PlayerSpawnEvent event) {
        event.getPlayer().sendPacket(this.simulatedDistancePacket);
        Optional.ofNullable(this.lobbyMap).map(LobbyMap::spawn).ifPresent(event.getPlayer()::teleport);
        this.navigationHelper.setItems(event.getPlayer());
    }
}
