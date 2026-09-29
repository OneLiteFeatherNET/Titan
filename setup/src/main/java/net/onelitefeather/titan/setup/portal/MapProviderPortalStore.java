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
package net.onelitefeather.titan.setup.portal;

import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.core.portal.Portal;

import java.util.List;

/**
 * The portals of the lobby map the setup server has loaded, saved through {@link MapProvider} so
 * that everything else in {@code map.json} is written back as it was.
 */
public final class MapProviderPortalStore implements PortalStore {

    private final MapProvider mapProvider;

    public MapProviderPortalStore(MapProvider mapProvider) {
        this.mapProvider = mapProvider;
    }

    @Override
    public List<Portal> portals() {
        return mapProvider.getActiveLobby().portals();
    }

    @Override
    public void save(List<Portal> portals) {
        // lobbyMapBuilder(map) copies spawn, name and authors, so only the portals change.
        mapProvider.saveMap(LobbyMap.lobbyMapBuilder(mapProvider.getActiveLobby()).portals(portals).build());
    }

    @Override
    public String world() {
        String name = mapProvider.getActiveLobby().name();
        return name == null ? PortalStore.super.world() : name;
    }
}
