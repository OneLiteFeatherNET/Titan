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
package net.onelitefeather.titan.apps.local;

import java.util.List;
import java.util.function.Consumer;
import net.minestom.server.coordinate.Pos;
import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.core.portal.Portal;
import org.mockito.Mockito;

/**
 * Stubs a mocked {@link MapProvider} with an active lobby, which the portal column reads at start.
 */
public final class ActiveLobby {

    private ActiveLobby() {
    }

    public static Consumer<MapProvider> with(List<Portal> portals) {
        return mapProvider -> Mockito.when(mapProvider.getActiveLobby()).thenReturn(new LobbyMap("test", new Pos(0, 65, 0), List.of(), portals));
    }

    public static Consumer<MapProvider> empty() {
        return with(List.of());
    }
}
