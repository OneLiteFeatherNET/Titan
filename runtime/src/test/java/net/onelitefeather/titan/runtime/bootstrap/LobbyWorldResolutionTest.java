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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers how {@link PlatformBeans} turns the {@link LobbyWorldChoice} implementations found by the
 * service loader into the world the {@link MapProvider} loads. Choices are handed in directly, so
 * no real service file is needed.
 */
@ExtendWith(MicrotusExtension.class)
class LobbyWorldResolutionTest {

    @TempDir
    Path base;

    private void createWorld(String name) throws IOException {
        Path directory = Files.createDirectories(this.base.resolve("worlds").resolve(name));
        Files.writeString(directory.resolve("map.json"), "{\"name\":\"" + name + "\"}");
    }

    private static LobbyWorldChoice choosing(String world) {
        return () -> Optional.of(world);
    }

    private MapProvider load(Env env, List<LobbyWorldChoice> choices) {
        InstanceContainer instance = env.process().instance().createInstanceContainer();
        return PlatformBeans.loadMapProvider(this.base, instance, choices);
    }

    @DisplayName("A choice selects its world even though another one is the default")
    @Test
    void choiceSelectsItsWorld(Env env) throws IOException {
        createWorld("world");
        createWorld("winter");

        MapProvider provider = load(env, List.of(choosing("winter")));

        Assertions.assertEquals("winter", provider.getActiveLobby().name(), "the chosen world must be the active lobby");
    }

    @DisplayName("Without a choice the default world is loaded")
    @Test
    void noChoiceLoadsTheDefaultWorld(Env env) throws IOException {
        createWorld("world");
        createWorld("winter");

        MapProvider provider = load(env, List.of());

        Assertions.assertEquals("world", provider.getActiveLobby().name(), "no choice must keep the default world");
    }

    @DisplayName("A choice without a world loads the default world")
    @Test
    void emptyChoiceLoadsTheDefaultWorld(Env env) throws IOException {
        createWorld("world");
        createWorld("winter");

        MapProvider provider = load(env, List.of(Optional::empty));

        Assertions.assertEquals("world", provider.getActiveLobby().name(), "an empty choice means the default world");
    }

    @DisplayName("A chosen world that does not exist aborts the load and names it")
    @Test
    void missingChosenWorldAbortsWithItsName(Env env) throws IOException {
        createWorld("world");

        RuntimeException failure = Assertions.assertThrows(RuntimeException.class, () -> load(env, List.of(choosing("winter"))));

        Assertions.assertTrue(failure.getMessage().contains("winter"), "the message must name the missing world, was: " + failure.getMessage());
    }

    @DisplayName("More than one choice aborts startup and names all of them")
    @Test
    void ambiguousChoicesAbortStartup(Env env) throws IOException {
        createWorld("world");
        LobbyWorldChoice first = choosing("a");
        LobbyWorldChoice second = choosing("b");

        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () -> load(env, List.of(first, second)));

        Assertions.assertTrue(failure.getMessage().contains(first.getClass().getName()), "the message must name each implementation, was: " + failure.getMessage());
    }
}
