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

import net.minestom.server.event.player.PlayerMoveEvent;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.map.LobbyMap;

import java.util.Optional;
import java.util.function.Consumer;

public final class PlayerMoveListener implements Consumer<PlayerMoveEvent> {
    private final AppConfig appConfig;
    private final LobbyMap lobbyMap;

    public PlayerMoveListener(AppConfig appConfig, LobbyMap lobbyMap) {
        this.appConfig = appConfig;
        this.lobbyMap = lobbyMap;
    }

    @Override
    public void accept(PlayerMoveEvent playerMoveEvent) {
        var player = playerMoveEvent.getPlayer();
        if (player.getInstance() == null)
            return;
        if (player.getPosition().y() < appConfig.minHeightBeforeTeleport()) {
            Optional.ofNullable(this.lobbyMap).map(LobbyMap::spawn).ifPresent(player::teleport);
            return;
        }
        if (player.getPosition().y() > appConfig.maxHeightBeforeTeleport()) {
            Optional.ofNullable(this.lobbyMap).map(LobbyMap::spawn).ifPresent(player::teleport);
        }
    }
}
