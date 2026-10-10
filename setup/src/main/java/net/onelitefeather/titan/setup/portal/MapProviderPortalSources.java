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
import net.onelitefeather.titan.common.map.MapEntry;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.core.portal.Portal;

import java.util.List;
import java.util.Optional;

/**
 * The other worlds under {@code worlds/}, read through the {@link MapProvider} without switching
 * the loaded world. Names come from the provider's list, never from the typed input.
 */
public final class MapProviderPortalSources implements PortalSources {

    private final MapProvider mapProvider;

    public MapProviderPortalSources(MapProvider mapProvider) {
        this.mapProvider = mapProvider;
    }

    @Override
    public String active() {
        return name(mapProvider.activeMap());
    }

    @Override
    public List<String> worlds() {
        String active = active();
        return mapProvider.getAvailableMaps().stream().filter(MapEntry::hasMapFile).map(MapProviderPortalSources::name).filter(name -> !name.equals(active)).sorted().toList();
    }

    @Override
    public Optional<List<Portal>> portalsOf(String world) {
        return mapProvider.getAvailableMaps().stream().filter(entry -> name(entry).equals(world)).findFirst().map(entry -> mapProvider.readMap(entry).map(LobbyMap::portals).orElse(List.of()));
    }

    private static String name(MapEntry entry) {
        return entry.path().getFileName().toString();
    }
}
