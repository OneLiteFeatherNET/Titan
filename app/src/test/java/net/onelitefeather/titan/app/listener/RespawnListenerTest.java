package net.onelitefeather.titan.app.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerRespawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.helper.NavigationHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MicrotusExtension.class)
class RespawnListenerTest {

    @DisplayName("test if the player gets the items after respawn")
    @Test
    void testRespawnListener(Env env) {
        Instance flatInstance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(flatInstance, Pos.ZERO).join();

        NavigationHelper navigationHelper = mock(NavigationHelper.class);

        MinecraftServer.getGlobalEventHandler().addListener(PlayerRespawnEvent.class, new RespawnListener(navigationHelper));
        MinecraftServer.getGlobalEventHandler().call(new PlayerRespawnEvent(player));

        verify(navigationHelper).setItems(eq(player));
    }

}
