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

import net.minestom.server.instance.InstanceContainer;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class MapProviderPortalTest {

    private static final String BOX = "{\"type\":\"box\",\"min\":{\"x\":0,\"y\":0,\"z\":0},\"max\":{\"x\":1,\"y\":1,\"z\":1}}";
    private static final String DISC = "{\"type\":\"disc\",\"center\":{\"x\":0,\"y\":0,\"z\":0},\"radius\":%s,\"normal\":{\"x\":0,\"y\":0,\"z\":%s}}";

    @TempDir
    Path base;

    @DisplayName("A map with an unknown shape type aborts the start instead of loading without portals")
    @Test
    void unknownShapeTypeAbortsTheStart(Env env) throws IOException {
        world("world", """
                {"name":"world","portals":[{"id":"p","task":"T","shape":{"type":"sphere"}}]}""");

        assertThrows(RuntimeException.class, () -> load(env), "an unreadable portal must not fall back to an empty map");
    }

    static Stream<Arguments> invalidPortals() {
        return Stream.of(
                Arguments.of("unknown shape", "{\"id\":\"p\",\"task\":\"T\",\"shape\":{\"type\":\"sphere\"}}", "sphere"), Arguments.of("radius 0", "{\"id\":\"p\",\"task\":\"T\",\"shape\":" + DISC.formatted(0, 1) + "}", "radius"), Arguments.of("min above max", "{\"id\":\"p\",\"task\":\"T\",\"shape\":{\"type\":\"box\",\"min\":{\"x\":5,\"y\":0,\"z\":0},\"max\":{\"x\":1,\"y\":1,\"z\":1}}}", "min.x"), Arguments.of("normal 0", "{\"id\":\"p\",\"task\":\"T\",\"shape\":" + DISC.formatted(2, 0) + "}", "normal"), Arguments.of("missing task", "{\"id\":\"p\",\"shape\":" + BOX + "}", "task"), Arguments.of("blank task", "{\"id\":\"p\",\"task\":\" \",\"shape\":" + BOX + "}", "task"), Arguments.of("missing id", "{\"task\":\"T\",\"shape\":" + BOX + "}", "id"), Arguments.of("duplicate id", "{\"id\":\"p\",\"task\":\"T\",\"shape\":" + BOX + "},{\"id\":\"p\",\"task\":\"T\",\"shape\":" + BOX + "}", "duplicate"));
    }

    @DisplayName("An invalid portal aborts the start with world, portal id and reason")
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPortals")
    void invalidPortalAbortsTheStart(String label, String portalJson, String reason, Env env) throws IOException {
        world("summer", "{\"name\":\"summer\",\"portals\":[" + portalJson + "]}");

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> load(env, "summer"), label + " must abort the start");

        String message = failure.getMessage();
        assertTrue(message.contains("summer"), "the message must name the world, was: " + message);
        assertTrue(message.contains(reason), "the message must give the reason '" + reason + "', was: " + message);
        if (label.contains("missing id")) {
            assertTrue(message.contains("portal #0"), "the message must identify the portal by its index, was: " + message);
        } else {
            assertTrue(message.contains("'p'"), "the message must name the portal id, was: " + message);
        }
    }

    @DisplayName("A valid portal starts and is part of the active lobby")
    @Test
    void validPortalStarts(Env env) throws IOException {
        world("world", "{\"name\":\"world\",\"portals\":[{\"id\":\"p\",\"task\":\"T\",\"shape\":" + BOX + "}]}");

        MapProvider provider = load(env);

        assertEquals(List.of(new Portal("p", new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), "T", null)), provider.getActiveLobby().portals());
    }

    @DisplayName("A map without portals loads with spawn, name and builders unchanged")
    @Test
    void mapWithoutPortalsLoadsUnchanged(Env env) throws IOException {
        world("world", "{\"name\":\"world\",\"spawn\":{\"x\":1.5,\"y\":65,\"z\":2.5,\"yaw\":90,\"pitch\":0},\"builders\":[\"alice\"]}");

        LobbyMap lobby = load(env).getActiveLobby();

        assertEquals("world", lobby.name());
        assertEquals(new Pos(1.5, 65, 2.5, 90, 0), lobby.spawn());
        assertEquals(List.of("alice"), lobby.builders());
        assertTrue(lobby.portals().isEmpty(), "a map without portals must have none");
    }

    @DisplayName("Saving a map writes its portals and the next load reads them again")
    @Test
    void saveWritesPortals(Env env) throws IOException {
        world("world", "{\"name\":\"world\",\"portals\":[{\"id\":\"p\",\"task\":\"T\",\"shape\":" + BOX + "}]}");
        MapProvider provider = load(env);

        provider.saveMap(LobbyMap.lobbyMapBuilder(provider.getActiveLobby()).spawn(new Pos(3, 70, 3)).build());

        assertEquals(1, provider.getActiveLobby().portals().size(), "a spawn change must keep the portal");
        assertTrue(Files.readString(this.base.resolve("worlds").resolve("world").resolve(MapEntry.MAP_FILE_NAME)).contains("\"portals\""), "the file must still hold the portals");
    }

    private static final PortalLabel LABEL = new PortalLabel(new Vec(12.5, 66, -3.5), "<gold>Survival<newline><online>/<max>", "<red>soon", new LabelSource.Service("Survival-1"), Billboard.FIXED, 90f);

    private static final String LABEL_JSON = """
            {"position":{"x":12.5,"y":66,"z":-3.5},"text":"<gold>Survival<newline><online>/<max>","offlineText":"<red>soon",
             "source":{"type":"service","name":"Survival-1"},"billboard":"fixed","yaw":90}""";

    private static String portalJson(String labelJson) {
        return "{\"name\":\"world\",\"portals\":[{\"id\":\"p\",\"task\":\"T\",\"shape\":" + BOX + ",\"label\":" + labelJson + "}]}";
    }

    @DisplayName("A portal label survives saving the map and loading it again")
    @Test
    void labelSurvivesSaveAndLoad(Env env) throws IOException {
        world("world", "{\"name\":\"world\",\"portals\":[{\"id\":\"p\",\"task\":\"T\",\"shape\":" + BOX + "}]}");
        MapProvider provider = load(env);
        Portal withLabel = new Portal("p", new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), "T", null, LABEL);

        provider.saveMap(LobbyMap.lobbyMapBuilder(provider.getActiveLobby()).portals(List.of(withLabel)).build());

        assertEquals(List.of(withLabel), provider.getActiveLobby().portals(), "the label must be unchanged after the reload in saveMap");
        assertEquals(List.of(withLabel), load(env).getActiveLobby().portals(), "a fresh provider reading the file must see the same label");
    }

    @DisplayName("Changing the spawn through the builder keeps the portal label")
    @Test
    void spawnChangeKeepsTheLabel(Env env) throws IOException {
        world("world", portalJson(LABEL_JSON));
        MapProvider provider = load(env);

        provider.saveMap(LobbyMap.lobbyMapBuilder(provider.getActiveLobby()).spawn(new Pos(3, 70, 3)).build());

        assertEquals(LABEL, provider.getActiveLobby().portals().getFirst().label(), "a spawn change must not drop the label");
        assertEquals(LABEL, load(env).getActiveLobby().portals().getFirst().label(), "the label must be on disk after the spawn change");
    }

    @DisplayName("An invalid label aborts the start with portal and field")
    @Test
    void invalidLabelAbortsTheStart(Env env) throws IOException {
        world("world", portalJson("""
                {"position":{"x":0,"y":0,"z":0},"text":"<gold>x</red>","source":{"type":"proxy"}}"""));

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> load(env), "an invalid label must abort the start");

        assertTrue(failure.getMessage().contains("'p'") && failure.getMessage().contains("label.source.type"), "message must name portal and field: " + failure.getMessage());
    }

    private MapProvider load(Env env) {
        return load(env, "world");
    }

    private MapProvider load(Env env, String world) {
        InstanceContainer instance = env.process().instance().createInstanceContainer();
        return MapProvider.create(this.base, instance, Optional.of(world));
    }

    private void world(String name, String json) throws IOException {
        Path directory = Files.createDirectories(this.base.resolve("worlds").resolve(name));
        Files.writeString(directory.resolve(MapEntry.MAP_FILE_NAME), json);
    }
}
