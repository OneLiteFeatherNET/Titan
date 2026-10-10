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

import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Portal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class MapProviderReadMapTest {

    private static final String BOX = "{\"type\":\"box\",\"min\":{\"x\":0,\"y\":0,\"z\":0},\"max\":{\"x\":1,\"y\":1,\"z\":1}}";
    private static final String ACTIVE = "world";
    private static final String SOURCE = "source";

    @TempDir
    Path base;

    @DisplayName("Reading another world returns its portals")
    @Test
    void readMapReturnsThePortalsOfAnotherWorld(Env env) throws IOException {
        world(ACTIVE, portals("active"));
        world(SOURCE, portals("survival", "creative"));
        MapProvider provider = load(env);

        Optional<LobbyMap> read = provider.readMap(entry(SOURCE));

        assertEquals(List.of(portal("survival"), portal("creative")), read.orElseThrow().portals(), "the portals of the source world must be read");
    }

    @DisplayName("Reading a world without a map file yields nothing")
    @Test
    void readMapOfWorldWithoutMapFileIsEmpty(Env env) throws IOException {
        world(ACTIVE, portals("active"));
        Files.createDirectories(this.base.resolve("worlds").resolve("empty"));
        MapProvider provider = load(env);

        assertFalse(provider.readMap(entry("empty")).isPresent(), "a directory without map.json has no map to read");
    }

    @DisplayName("Reading a world with an unknown portal shape fails with the world name")
    @Test
    void readMapRejectsUnknownPortalShape(Env env) throws IOException {
        world(ACTIVE, portals("active"));
        world(SOURCE, "{\"name\":\"source\",\"portals\":[{\"id\":\"p\",\"task\":\"T\",\"shape\":{\"type\":\"sphere\"}}]}");
        MapProvider provider = load(env);

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> provider.readMap(entry(SOURCE)), "an invalid source must not be read as empty");

        assertTrue(failure.getMessage().contains(SOURCE), "the message must name the source world, was: " + failure.getMessage());
    }

    @DisplayName("Reading a world with broken JSON fails instead of returning nothing")
    @Test
    void readMapRejectsBrokenJson(Env env) throws IOException {
        world(ACTIVE, portals("active"));
        world(SOURCE, "{\"name\":\"source\",\"portals\":[");
        MapProvider provider = load(env);

        assertThrows(IllegalStateException.class, () -> provider.readMap(entry(SOURCE)), "broken JSON must not be read as an empty map");
    }

    @DisplayName("Reading another world keeps the active world and its portals")
    @Test
    void readMapLeavesTheActiveWorldUntouched(Env env) throws IOException {
        world(ACTIVE, portals("active"));
        world(SOURCE, portals("survival"));
        MapProvider provider = load(env);

        provider.readMap(entry(SOURCE));

        assertEquals(ACTIVE, provider.getActiveLobby().name(), "the active lobby must stay the loaded world");
        assertEquals(this.base.resolve("worlds").resolve(ACTIVE), provider.activeMap().path(), "the active map must stay the loaded world");
        assertEquals(List.of(portal("active")), provider.getActiveLobby().portals(), "the active portals must not change");
    }

    @DisplayName("Reading another world never changes its map file")
    @Test
    void readMapLeavesTheSourceFileByteIdentical(Env env) throws IOException {
        world(ACTIVE, portals("active"));
        world(SOURCE, portals("survival"));
        Path sourceFile = this.base.resolve("worlds").resolve(SOURCE).resolve(MapEntry.MAP_FILE_NAME);
        byte[] before = Files.readAllBytes(sourceFile);
        MapProvider provider = load(env);

        provider.readMap(entry(SOURCE));

        assertArrayEquals(before, Files.readAllBytes(sourceFile), "the source map file must be byte-identical after reading");
    }

    private static String portals(String... ids) {
        StringBuilder json = new StringBuilder("{\"name\":\"world\",\"portals\":[");
        for (int index = 0; index < ids.length; index++) {
            json.append(index == 0 ? "" : ",").append("{\"id\":\"").append(ids[index]).append("\",\"task\":\"T\",\"shape\":").append(BOX).append("}");
        }
        return json.append("]}").toString();
    }

    private static Portal portal(String id) {
        return new Portal(id, new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), "T", null);
    }

    private MapProvider load(Env env) {
        InstanceContainer instance = env.process().instance().createInstanceContainer();
        return MapProvider.create(this.base, instance, Optional.of(ACTIVE));
    }

    private MapEntry entry(String world) {
        return new MapEntry(this.base.resolve("worlds").resolve(world));
    }

    private void world(String name, String json) throws IOException {
        Path directory = Files.createDirectories(this.base.resolve("worlds").resolve(name));
        Files.writeString(directory.resolve(MapEntry.MAP_FILE_NAME), json);
    }
}
