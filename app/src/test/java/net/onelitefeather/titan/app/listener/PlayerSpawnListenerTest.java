package net.onelitefeather.titan.app.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.UpdateSimulationDistancePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.helper.NavigationHelper;
import net.onelitefeather.titan.common.config.InternalAppConfig;
import net.onelitefeather.titan.common.map.LobbyMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.*;

@ExtendWith(MicrotusExtension.class)
class PlayerSpawnListenerTest {

    @DisplayName("Test if simulation cached packet is sent")
    @Test
    void testSendSimulationCachedPacket(Env env) {
        Instance flatInstance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(flatInstance, Pos.ZERO).join();

        LobbyMap lobbyMap = mock(LobbyMap.class);
        NavigationHelper navigationHelper = mock(NavigationHelper.class);
        Collector<UpdateSimulationDistancePacket> updateSimulationDistancePacketCollector = connection.trackIncoming(UpdateSimulationDistancePacket.class);

        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, new PlayerSpawnListener(InternalAppConfig.defaultConfig(), lobbyMap, navigationHelper));
        MinecraftServer.getGlobalEventHandler().call(new PlayerSpawnEvent(player, flatInstance, true));

        updateSimulationDistancePacketCollector.assertSingle();
        Assertions.assertEquals(InternalAppConfig.defaultConfig().simulationDistance(), updateSimulationDistancePacketCollector.collect().getFirst().simulationDistance());
    }

    @DisplayName("Test if player is teleported to lobby map spawn")
    @Test
    void testPlayerTeleport(Env env) {
        Instance flatInstance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = spy(connection.connect(flatInstance, Pos.ZERO).join());

        LobbyMap lobbyMap = mock(LobbyMap.class);
        var lobbyMapSpawn = new Pos(1, 2, 3);
        when(lobbyMap.getSpawn()).thenReturn(lobbyMapSpawn);
        NavigationHelper navigationHelper = mock(NavigationHelper.class);

        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, new PlayerSpawnListener(InternalAppConfig.defaultConfig(), lobbyMap, navigationHelper));
        MinecraftServer.getGlobalEventHandler().call(new PlayerSpawnEvent(player, flatInstance, true));

        verify(player, times(1)).teleport(eq(lobbyMapSpawn));
    }

    @DisplayName("Test if player items are set")
    @Test
    void testPlayerItemsSet(Env env) {
        Instance flatInstance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(flatInstance, Pos.ZERO).join();

        LobbyMap lobbyMap = mock(LobbyMap.class);
        NavigationHelper navigationHelper = mock(NavigationHelper.class);

        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, new PlayerSpawnListener(InternalAppConfig.defaultConfig(), lobbyMap, navigationHelper));
        MinecraftServer.getGlobalEventHandler().call(new PlayerSpawnEvent(player, flatInstance, true));

        verify(navigationHelper, times(1)).setItems(eq(player));
    }

}
