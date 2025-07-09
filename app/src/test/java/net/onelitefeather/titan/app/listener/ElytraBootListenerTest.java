package net.onelitefeather.titan.app.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.config.InternalAppConfig;
import net.onelitefeather.titan.common.utils.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.*;

@ExtendWith(MicrotusExtension.class)
class ElytraBootListenerTest {


    @DisplayName("Test if the ElytraBoostListener call setVelocity on the player")
    @Test
    void testIsVelocitySet(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = spy(env.createPlayer(flatInstance));
        MinecraftServer.getGlobalEventHandler().addListener(PlayerUseItemEvent.class, new ElytraBoostListener(InternalAppConfig.defaultConfig()));
        when(player.isFlyingWithElytra()).thenReturn(true);

        PlayerUseItemEvent playerUseItemEvent = new PlayerUseItemEvent(player, PlayerHand.MAIN, Items.PLAYER_FIREWORK, 1);
        MinecraftServer.getGlobalEventHandler().call(playerUseItemEvent);

        verify(player, times(1)).setVelocity(any());
    }

    @DisplayName("Test if the ElytraBoostListener does not call setVelocity on the player because the player use a different item")
    @Test
    void testPlayerIsFlyingHasNoFireworkInHand(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = spy(env.createPlayer(flatInstance));
        MinecraftServer.getGlobalEventHandler().addListener(PlayerUseItemEvent.class, new ElytraBoostListener(InternalAppConfig.defaultConfig()));
        when(player.isFlyingWithElytra()).thenReturn(true);

        PlayerUseItemEvent playerUseItemEvent = new PlayerUseItemEvent(player, PlayerHand.MAIN, Items.NAVIGATOR_BLANK_ITEM_STACK, 1);
        MinecraftServer.getGlobalEventHandler().call(playerUseItemEvent);

        verify(player, times(0)).setVelocity(any());
    }


    @DisplayName("Test if the ElytraBoostListener does not call setVelocity on the player because the player use a different item")
    @Test
    void testPlayerIsNotFlyingHasFireworkInHand(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = spy(env.createPlayer(flatInstance));
        MinecraftServer.getGlobalEventHandler().addListener(PlayerUseItemEvent.class, new ElytraBoostListener(InternalAppConfig.defaultConfig()));
        when(player.isFlyingWithElytra()).thenReturn(false);

        PlayerUseItemEvent playerUseItemEvent = new PlayerUseItemEvent(player, PlayerHand.MAIN, Items.PLAYER_FIREWORK, 1);
        MinecraftServer.getGlobalEventHandler().call(playerUseItemEvent);

        verify(player, times(0)).setVelocity(any());
    }

}
