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
package net.onelitefeather.titan.common.map;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Portal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The setup server rewrites a map through {@code lobbyMapBuilder(map)}; a copy path that dropped
 * the portals would silently delete them on the next {@code setspawn}.
 */
class LobbyMapPortalsTest {

    private static final List<Portal> PORTALS = List.of(new Portal("a", new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), "T", null));

    private static LobbyMap withPortals() {
        return LobbyMap.lobbyMapBuilder().name("world").spawn(new Pos(1, 2, 3)).author("alice").portals(PORTALS).build();
    }

    @DisplayName("A map built without portals reports an empty list")
    @Test
    void portalsDefaultToEmpty() {
        assertTrue(LobbyMap.lobbyMapBuilder().build().portals().isEmpty(), "portals() must never be null");
        assertTrue(new LobbyMap("world", null, null).portals().isEmpty(), "the three-argument constructor stays usable");
    }

    @DisplayName("The builder keeps the given portals")
    @Test
    void builderKeepsPortals() {
        assertEquals(PORTALS, withPortals().portals());
    }

    @DisplayName("Copying a map keeps its portals")
    @Test
    void copyKeepsPortals() {
        assertEquals(PORTALS, LobbyMap.lobbyMapBuilder(withPortals()).build().portals());
    }

    @DisplayName("Setting the spawn on a copy keeps the portals")
    @Test
    void settingSpawnKeepsPortals() {
        LobbyMap copy = LobbyMap.lobbyMapBuilder(withPortals()).spawn(new Pos(9, 9, 9)).build();

        assertEquals(PORTALS, copy.portals(), "setspawn must not delete portals");
        assertEquals(new Pos(9, 9, 9), copy.spawn());
    }

    @DisplayName("Setting the name on a copy keeps the portals")
    @Test
    void settingNameKeepsPortals() {
        LobbyMap copy = LobbyMap.lobbyMapBuilder(withPortals()).name("winter").build();

        assertEquals(PORTALS, copy.portals(), "setname must not delete portals");
        assertEquals("winter", copy.name());
    }

    @DisplayName("Setting the author on a copy keeps the portals")
    @Test
    void settingAuthorKeepsPortals() {
        LobbyMap copy = LobbyMap.lobbyMapBuilder(withPortals()).author("bob").build();

        assertEquals(PORTALS, copy.portals(), "setauthor must not delete portals");
        assertEquals(List.of("bob"), copy.builders());
    }

    @DisplayName("Copying a null map yields an empty builder result")
    @Test
    void copyOfNullHasNoPortals() {
        assertTrue(LobbyMap.lobbyMapBuilder(null).build().portals().isEmpty());
    }
}
