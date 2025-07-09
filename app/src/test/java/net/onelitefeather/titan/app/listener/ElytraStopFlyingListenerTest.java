package net.onelitefeather.titan.app.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.*;

@ExtendWith(MicrotusExtension.class)
class ElytraStopFlyingListenerTest {

    @DisplayName("Test if the is air item is set in the offhand when the player stops flying with the elytra")
    @Test
    void testElytraStopFlyingListener(Env env) {
        Instance flatInstance = env.createFlatInstance();
        var realPlayer = env.createPlayer(flatInstance);
        var realInventory = realPlayer.getInventory();
        Player player = spy(realPlayer);
        doReturn(spy(realInventory)).when(player).getInventory();
        env.tick();
        MinecraftServer.getGlobalEventHandler().addListener(PlayerStopFlyingWithElytraEvent.class, new ElytraStopFlyingListener());
        MinecraftServer.getGlobalEventHandler().call(new PlayerStopFlyingWithElytraEvent(player));

        Assertions.assertEquals(ItemStack.AIR, player.getItemInOffHand());
        verify(player.getInventory(), atLeastOnce()).update();
    }

}
