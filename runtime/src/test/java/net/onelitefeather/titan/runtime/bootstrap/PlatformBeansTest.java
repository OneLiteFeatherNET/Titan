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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import net.minestom.server.coordinate.Pos;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.LobbySpawn;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.runtime.deliver.TracedDeliver;
import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.List;

/**
 * Unit coverage for {@link PlatformBeans#lobbySpawn(MapProvider)}: its {@link LobbySpawn} is read
 * lazily on every {@code position()} call, so a map reload is picked up without rebuilding it.
 *
 * <p>Mocks {@link MapProvider} rather than a real one, so this runs without a Minestom server or
 * the filesystem {@code worlds/} it reads; {@code PlatformBeans#mapProvider}/{@code #featureFlags}
 * are deliberately not covered here, since both touch real process state a unit test must not
 * depend on. {@link PlatformBeans#commandManager()} is the exception: it is covered through
 * Cyano's {@link Env}, a fresh fake {@code ServerProcess} per test, so no real server is needed.
 */
@ExtendWith(MicrotusExtension.class)
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

    @DisplayName("The commandManager bean is the server process's own CommandManager")
    @Test
    void commandManagerBeanIsTheServerProcesssCommandManager(Env env) {
        Assertions.assertSame(env.process().command(), this.platformBeans.commandManager(), "the bean must not wrap or replace the server's CommandManager");
    }

    @DisplayName("The deliver bean wraps the platform deliver, so every transfer is traced")
    @Test
    void deliverBeanIsTraced() {
        Assertions.assertInstanceOf(TracedDeliver.class, this.platformBeans.deliver(Telemetry.noop()), "portals and the navigator must both get the traced deliver");
    }

    @DisplayName("The telemetry bean reports under the net.onelitefeather.titan scope of the given OpenTelemetry")
    @Test
    void telemetryBeanUsesTheTitanScope() {
        InMemorySpanExporter exporter = InMemorySpanExporter.create();
        try (SdkTracerProvider tracerProvider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build()) {
            Telemetry telemetry = this.platformBeans.telemetry(OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build());

            telemetry.inSpan("test.op", Attributes.empty(), () -> {
            });

            Assertions.assertEquals(Telemetry.SCOPE, exporter.getFinishedSpanItems().getFirst().getInstrumentationScopeInfo().getName());
        }
    }
}
