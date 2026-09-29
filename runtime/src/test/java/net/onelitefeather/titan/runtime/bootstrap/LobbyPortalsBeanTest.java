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
package net.onelitefeather.titan.runtime.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.core.module.LobbyWorldChoice;
import net.onelitefeather.titan.core.portal.LobbyPortals;
import net.onelitefeather.titan.core.portal.Portal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * The {@link LobbyPortals} bean reads the portals of whichever world the {@link MapProvider}
 * loaded, so a seasonal world brings its own portals without any extra wiring.
 */
@ExtendWith(MicrotusExtension.class)
class LobbyPortalsBeanTest {

    @TempDir
    Path base;

    private final PlatformBeans platformBeans = new PlatformBeans();

    private void createWorld(String name, String portalsJson) throws IOException {
        Path directory = Files.createDirectories(this.base.resolve("worlds").resolve(name));
        Files.writeString(directory.resolve("map.json"), "{\"name\":\"" + name + "\"" + portalsJson + "}");
    }

    private static String portal(String id) {
        return ",\"portals\":[{\"id\":\"" + id + "\",\"task\":\"Survival\",\"shape\":{\"type\":\"box\",\"min\":{\"x\":0,\"y\":0,\"z\":0},\"max\":{\"x\":1,\"y\":1,\"z\":1}}}]";
    }

    private LobbyPortals portalsOf(Env env, Optional<String> season) {
        InstanceContainer instance = env.process().instance().createInstanceContainer();
        LobbyWorldChoice choice = () -> season;
        MapProvider provider = PlatformBeans.loadMapProvider(this.base, instance, List.of(choice));
        return this.platformBeans.lobbyPortals(provider);
    }

    private static List<String> ids(LobbyPortals portals) {
        return portals.portals().stream().map(Portal::id).toList();
    }

    @DisplayName("The bean lists the portals of the active lobby")
    @Test
    void listsThePortalsOfTheActiveLobby(Env env) throws IOException {
        createWorld("world", portal("survival"));
        createWorld("winter", portal("winter-gate"));

        LobbyPortals portals = portalsOf(env, Optional.empty());

        Assertions.assertEquals(List.of("survival"), ids(portals), "the default world's portals must be served");
    }

    @DisplayName("In a season world the bean lists that world's portals")
    @Test
    void seasonWorldBringsItsOwnPortals(Env env) throws IOException {
        createWorld("world", portal("survival"));
        createWorld("winter", portal("winter-gate"));

        LobbyPortals portals = portalsOf(env, Optional.of("winter"));

        Assertions.assertEquals(List.of("winter-gate"), ids(portals), "the season world's map.json decides, not the default world's");
    }

    @DisplayName("A world without portals yields an empty list")
    @Test
    void worldWithoutPortalsYieldsEmptyList(Env env) throws IOException {
        createWorld("world", "");

        LobbyPortals portals = portalsOf(env, Optional.empty());

        Assertions.assertTrue(portals.portals().isEmpty(), "no portals in the map means an empty list, not null");
    }
}
