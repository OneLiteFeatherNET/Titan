package net.onelitefeather.titan.app.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.map.MapProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MicrotusExtension.class)
class PlayerConfigurationListenerTest {

    @DisplayName("Test set spawning instance")
    @Test
    void testSetSpawningInstance(Env env) {
        MapProvider mapProvider = mock(MapProvider.class);


        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, new PlayerConfigurationListener(mapProvider));
        InstanceContainer instance = (InstanceContainer) env.createFlatInstance();

        when(mapProvider.getInstance()).thenReturn(instance);
        Player player = env.createPlayer(instance);
        AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);
        MinecraftServer.getGlobalEventHandler().call(event);

        Mockito.verify(mapProvider, atLeastOnce()).getInstance();
        assertEquals(instance, event.getSpawningInstance());
    }

}
