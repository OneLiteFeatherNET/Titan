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
package net.onelitefeather.titan.app.bootstrap;

import io.avaje.config.Configuration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.onelitefeather.titan.app.module.LobbySpawn;
import net.onelitefeather.titan.common.config.ConfigSections;
import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit coverage for {@link PlatformBeans#lobbySpawn(MapProvider)}: the bean method's own Javadoc
 * promises the {@link LobbySpawn} it returns is "read lazily, on every
 * {@code LobbySpawn#position()} call, so a map reload is picked up without rebuilding any
 * module" - this proves both halves of that promise. Builds {@code PlatformBeans} directly and
 * mocks {@link MapProvider} rather than a real one, so this runs without a Minestom server or the
 * filesystem {@code worlds/} a real {@code MapProvider} reads.
 *
 * <p>Also covers {@link PlatformBeans#configuration()} and {@link
 * PlatformBeans#configSections(Configuration)}: run against this module's own working directory,
 * which - unlike the repository root before this change - never has an {@code application.yaml}
 * or a leftover {@code app.json} of its own, so both beans must build cleanly and a section must
 * fall back to its defaults, exactly like the {@code lobby-module-config} spec's "Ohne
 * Konfigurationsdatei gelten die Standardwerte" scenario.
 */
class PlatformBeansTest {

    private final PlatformBeans platformBeans = new PlatformBeans();

    @DisplayName("Building the LobbySpawn bean does not query the MapProvider")
    @Test
    void buildingTheBeanDoesNotQueryTheMapProvider() {
        MapProvider mapProvider = Mockito.mock(MapProvider.class);

        this.platformBeans.lobbySpawn(mapProvider);

        Mockito.verifyNoInteractions(mapProvider);
    }

    @DisplayName("Each position() call returns the current active lobby's spawn, not one captured when the bean was built")
    @Test
    void eachPositionCallReturnsTheCurrentActiveLobbysSpawn() {
        MapProvider mapProvider = Mockito.mock(MapProvider.class);
        Pos firstSpawn = new Pos(1, 65, 1);
        Pos secondSpawn = new Pos(9, 70, 9);
        LobbyMap firstLobby = new LobbyMap("first", firstSpawn, List.of());
        LobbyMap secondLobby = new LobbyMap("second", secondSpawn, List.of());
        Mockito.when(mapProvider.getActiveLobby()).thenReturn(firstLobby, secondLobby);
        LobbySpawn lobbySpawn = this.platformBeans.lobbySpawn(mapProvider);

        Pos positionBeforeSwitch = lobbySpawn.position();
        Pos positionAfterSwitch = lobbySpawn.position();

        Assertions.assertEquals(firstSpawn, positionBeforeSwitch, "the first call must return the active lobby's spawn at that time");
        Assertions.assertEquals(secondSpawn, positionAfterSwitch, "the next call must return the switched-to active lobby's spawn, proving position() re-reads MapProvider every time instead of caching");
    }

    private record Probe(int value) {
    }

    @DisplayName("configuration() and configSections() build cleanly with no application.yaml present, and a section falls back to its defaults")
    @Test
    void configurationAndConfigSectionsBuildCleanlyWithNoFilePresent() {
        Path workingDir = Path.of("").toAbsolutePath();
        Assertions.assertFalse(Files.exists(workingDir.resolve("application.yaml")), "this module's own directory must not have an application.yaml of its own");
        Assertions.assertFalse(Files.exists(workingDir.resolve("app.json")), "this module's own directory must not have a leftover app.json of its own");

        Configuration configuration = this.platformBeans.configuration();
        ConfigSections configSections = this.platformBeans.configSections(configuration);

        Probe probe = configSections.section("platform-beans-test-probe", Probe.class, new Probe(42));
        Assertions.assertEquals(42, probe.value(), "with no application.yaml in the working directory, a section must fall back to its defaults");
    }
}
