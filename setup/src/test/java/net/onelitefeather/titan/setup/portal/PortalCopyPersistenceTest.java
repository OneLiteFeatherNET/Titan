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
import net.minestom.server.instance.InstanceContainer;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapEntry;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.setup.portal.editor.PortalCopier;
import net.onelitefeather.titan.setup.portal.editor.PortalEditor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Copy and save-all against real map files in two temporary worlds. */
@ExtendWith(MicrotusExtension.class)
class PortalCopyPersistenceTest {

    private static final UUID PLAYER = new UUID(0, 7);
    private static final String LOBBY = """
            {"name":"lobby","portals":[{"id":"survival","task":"Survival","shape":{"type":"box","min":{"x":10,"y":64,"z":10},"max":{"x":14,"y":68,"z":11}}},
             {"id":"creative","task":"Creative","shape":{"type":"box","min":{"x":20,"y":64,"z":20},"max":{"x":24,"y":68,"z":21}}}]}""";
    private static final String WINTER = """
            {"name":"world","spawn":{"x":1.5,"y":65,"z":2.5,"yaw":90,"pitch":0},"builders":["alice"],
             "portals":[{"id":"old","task":"Old task","shape":{"type":"box","min":{"x":0,"y":0,"z":0},"max":{"x":1,"y":1,"z":1}}}]}""";

    @TempDir
    Path base;

    @Test
    @DisplayName("Copying and saving never changes the source map file")
    void sourceFileStaysByteIdentical(Env env) throws IOException {
        Path source = world("lobby", LOBBY);
        world("world", WINTER);
        byte[] before = Files.readAllBytes(source);
        MapProvider provider = load(env);
        MapProviderPortalStore store = new MapProviderPortalStore(provider);
        PortalEditor editor = new PortalEditor(store);
        new PortalCopier(editor, new MapProviderPortalSources(provider), store).copy(PLAYER, "lobby");
        editor.saveAll(PLAYER);

        assertArrayEquals(before, Files.readAllBytes(source), "the source map file must be byte-identical");
    }

    @Test
    @DisplayName("Saving copied portals keeps the target's portals, spawn, name and authors on disk")
    void targetKeepsSpawnNameAndAuthors(Env env) throws IOException {
        world("lobby", LOBBY);
        world("world", WINTER);
        MapProvider provider = load(env);
        MapProviderPortalStore store = new MapProviderPortalStore(provider);
        PortalEditor editor = new PortalEditor(store);
        new PortalCopier(editor, new MapProviderPortalSources(provider), store).copy(PLAYER, "lobby");
        editor.saveAll(PLAYER);

        LobbyMap reloaded = load(env).getActiveLobby();

        assertEquals(List.of("old", "survival", "creative"), reloaded.portals().stream().map(Portal::id).toList(), "the target keeps its portal and gains the copies");
        assertEquals(new Pos(1.5, 65, 2.5, 90, 0), reloaded.spawn(), "spawn must be untouched");
        assertEquals("world", reloaded.name(), "name must be untouched");
        assertEquals(List.of("alice"), reloaded.builders(), "authors must be untouched");
    }

    @Test
    @DisplayName("The worlds offered for copying are the other folders that have a map file")
    void sourcesListOnlyOtherFoldersWithAMapFile(Env env) throws IOException {
        world("lobby", LOBBY);
        world("world", WINTER);
        Files.createDirectories(this.base.resolve("worlds").resolve("empty"));
        MapProvider provider = MapProvider.create(this.base, env.process().instance().createInstanceContainer(), stream -> stream.map(MapEntry::new).toList());
        assertEquals("world", new MapProviderPortalSources(provider).active(), "the loaded world is the one the pool picks by its default name");

        List<String> worlds = new MapProviderPortalSources(provider).worlds();

        assertEquals(List.of("lobby"), worlds, "the loaded world and the folder without map.json are not offered");
    }

    private MapProvider load(Env env) {
        InstanceContainer instance = env.process().instance().createInstanceContainer();
        return MapProvider.create(this.base, instance, Optional.of("world"));
    }

    private Path world(String name, String json) throws IOException {
        Path directory = Files.createDirectories(this.base.resolve("worlds").resolve(name));
        return Files.writeString(directory.resolve(MapEntry.MAP_FILE_NAME), json);
    }
}
