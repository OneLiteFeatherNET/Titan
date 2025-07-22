/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
		Pos pos = Optional.of(this.mapProvider).map(MapProvider::getActiveLobby).map(LobbyMap::getSpawn).orElse(null);
		if (pos == null)
			return;
		event.getPlayer().setRespawnPoint(pos);
	}
}
