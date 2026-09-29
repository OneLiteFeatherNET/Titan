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

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapEntry;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Portal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MicrotusExtension.class)
class MapProviderPortalStoreTest {

    private static final Portal OLD = new Portal("old", new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), "Old task", null);
    private static final Portal NEW = new Portal("new", new Box(new Vec(5, 5, 5), new Vec(6, 6, 6)), "New task", "perm.new");

    @TempDir
    Path base;

    @DisplayName("A saved portal survives a fresh load together with spawn, name and authors")
    @Test
    void savedPortalIsLoadedAgainWithTheRestOfTheMap(Env env) throws IOException {
        MapProviderPortalStore store = new MapProviderPortalStore(providerWithOldPortal(env));

        List<Portal> next = new ArrayList<>(store.portals());
        next.add(NEW);
        store.save(next);

        LobbyMap reloaded = freshProvider(env).getActiveLobby();
        assertEquals(List.of(OLD, NEW), reloaded.portals(), "old and new portal must be on disk");
        assertEquals(new Pos(1.5, 65, 2.5, 90, 0), reloaded.spawn(), "spawn must be untouched");
        assertEquals("world", reloaded.name(), "name must be untouched");
        assertEquals(List.of("alice"), reloaded.builders(), "authors must be untouched");
    }

    @DisplayName("Removing a portal leaves spawn, name and authors alone")
    @Test
    void removalKeepsTheRestOfTheMap(Env env) throws IOException {
        MapProviderPortalStore store = new MapProviderPortalStore(providerWithOldPortal(env));

        store.save(List.of());

        LobbyMap reloaded = freshProvider(env).getActiveLobby();
        assertEquals(List.of(), reloaded.portals(), "the portal must be gone");
        assertEquals(new Pos(1.5, 65, 2.5, 90, 0), reloaded.spawn(), "spawn must be untouched");
        assertEquals(List.of("alice"), reloaded.builders(), "authors must be untouched");
    }

    @DisplayName("The store reports the portals of the active lobby and its name as the world")
    @Test
    void readsThePortalsAndNameOfTheActiveLobby(Env env) throws IOException {
        MapProviderPortalStore store = new MapProviderPortalStore(providerWithOldPortal(env));

        assertEquals(List.of(OLD), store.portals(), "portals come from the active lobby");
        assertEquals("world", store.world(), "the world is the map name");
    }

    @DisplayName("/setup map setspawn keeps the saved portals")
    @Test
    void setSpawnPathKeepsThePortals(Env env) throws IOException {
        MapProvider provider = providerWithOldPortal(env);

        provider.saveMap(LobbyMap.lobbyMapBuilder(provider.getActiveLobby()).spawn(new Pos(9, 70, 9)).build());

        LobbyMap reloaded = freshProvider(env).getActiveLobby();
        assertEquals(new Pos(9, 70, 9), reloaded.spawn(), "the new spawn must be saved");
        assertEquals(List.of(OLD), reloaded.portals(), "setspawn must not delete portals");
    }

    private MapProvider providerWithOldPortal(Env env) throws IOException {
        Path directory = Files.createDirectories(base.resolve("worlds").resolve("world"));
        Files.writeString(directory.resolve(MapEntry.MAP_FILE_NAME), """
                {"name":"world","spawn":{"x":1.5,"y":65,"z":2.5,"yaw":90,"pitch":0},"builders":["alice"],
                 "portals":[{"id":"old","task":"Old task","shape":{"type":"box","min":{"x":0,"y":0,"z":0},"max":{"x":1,"y":1,"z":1}}}]}""");
        return freshProvider(env);
    }

    private MapProvider freshProvider(Env env) {
        return MapProvider.create(base, env.process().instance().createInstanceContainer(), Optional.of("world"));
    }
}
