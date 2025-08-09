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
package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapProvider;

import java.util.Optional;
import java.util.function.Consumer;

public final class PlayerConfigurationListener implements Consumer<AsyncPlayerConfigurationEvent> {

    private final MapProvider mapProvider;

    public PlayerConfigurationListener(MapProvider mapProvider) {
        this.mapProvider = mapProvider;
    }

    @Override
    public void accept(AsyncPlayerConfigurationEvent event) {
        Optional.ofNullable(this.mapProvider).map(MapProvider::getInstance).ifPresent(event::setSpawningInstance);
        Optional.of(this.mapProvider).map(MapProvider::getActiveLobby).map(LobbyMap::getSpawn).ifPresent(event.getPlayer()::setRespawnPoint);
    }
}
