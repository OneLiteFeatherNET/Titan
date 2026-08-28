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
package net.onelitefeather.titan.setup.listener;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapProvider;

import java.util.Optional;
import java.util.function.Consumer;

public class PlayerConfigurationListener implements Consumer<AsyncPlayerConfigurationEvent> {

    private final MapProvider mapProvider;

    public PlayerConfigurationListener(MapProvider mapProvider) {
        this.mapProvider = mapProvider;
    }

    @Override
    public void accept(AsyncPlayerConfigurationEvent event) {
        event.setSpawningInstance(this.mapProvider.getInstance());
        event.getPlayer().setGameMode(GameMode.CREATIVE);
        Pos pos = Optional.of(this.mapProvider).map(MapProvider::getActiveLobby).map(LobbyMap::spawn).orElse(null);
        if (pos == null)
            return;
        event.getPlayer().setRespawnPoint(pos);
    }
}
