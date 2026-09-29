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

import io.avaje.config.Config;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ServiceLoader;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.core.module.LobbyWorldChoice;
import net.onelitefeather.titan.feature.season.SeasonChoices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * The wiring between the season column and the map loader (design D2): the season choice feeds
 * {@link PlatformBeans}, and the service loader finds it. The choice is built with a fixed clock
 * because the one the service loader instantiates reads the system time.
 */
@ExtendWith(MicrotusExtension.class)
class SeasonLobbyWorldTest {

    private static final Clock IN_WINTER = Clock.fixed(Instant.parse("2026-12-24T12:00:00Z"), ZoneOffset.UTC);
    private static final Clock IN_SUMMER = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path base;

    private String originalZone;

    @BeforeEach
    void setUp() throws IOException {
        this.originalZone = Config.get("seasons.zone");
        createWorld("world");
        createWorld("winter-map");
        Config.setProperty("seasons.winter.world", "winter-map");
        Config.setProperty("seasons.winter.from", "2026-12-01T00:00:00");
        Config.setProperty("seasons.winter.to", "2027-01-07T00:00:00");
    }

    @AfterEach
    void tearDown() {
        Config.asProperties().stringPropertyNames().stream().filter(key -> key.startsWith("seasons.")).forEach(Config::clearProperty);
        Config.setProperty("seasons.zone", this.originalZone);
    }

    private void createWorld(String name) throws IOException {
        Path directory = Files.createDirectories(this.base.resolve("worlds").resolve(name));
        Files.writeString(directory.resolve("map.json"), "{\"name\":\"" + name + "\"}");
    }

    private MapProvider load(Env env, Clock clock) {
        InstanceContainer instance = env.process().instance().createInstanceContainer();
        return PlatformBeans.loadMapProvider(this.base, instance, List.of(SeasonChoices.at(clock, this.base.resolve("worlds"))));
    }

    @DisplayName("Inside the window the lobby loads the season world")
    @Test
    void insideTheWindowTheSeasonWorldIsLoaded(Env env) {
        Assertions.assertEquals("winter-map", load(env, IN_WINTER).getActiveLobby().name(), "the season world must be the active lobby");
    }

    @DisplayName("Outside the window the lobby loads the default world")
    @Test
    void outsideTheWindowTheDefaultWorldIsLoaded(Env env) {
        Assertions.assertEquals("world", load(env, IN_SUMMER).getActiveLobby().name(), "no active season must keep the default world");
    }

    @DisplayName("The service loader finds exactly the season choice on the cloudnet classpath")
    @Test
    void serviceLoaderFindsTheSeasonChoice() {
        List<String> found = ServiceLoader.load(LobbyWorldChoice.class).stream().map(provider -> provider.type().getSimpleName()).toList();

        Assertions.assertEquals(List.of("SeasonWorldChoice"), found, "the merged service file must list the season choice once");
    }
}
