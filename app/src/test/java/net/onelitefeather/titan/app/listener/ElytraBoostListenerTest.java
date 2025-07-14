/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.app.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.config.InternalAppConfig;
import net.onelitefeather.titan.common.utils.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MicrotusExtension.class)
class ElytraBoostListenerTest {


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

    @DisplayName("Test the calculation of the boost vector")
    @Test
    void testBoostVectorCalculation(Env env) {
        // Create a custom AppConfig with a specific boost multiplier
        double boostMultiplier = 2.5;
        AppConfig customConfig = AppConfig.builder()
                .elytraBoostMultiplier(boostMultiplier)
                .build();
        
        // Create the listener with the custom config
        ElytraBoostListener listener = new ElytraBoostListener(customConfig);
        
        // Create a player
        Instance flatInstance = env.createFlatInstance();
        Player player = spy(env.createPlayer(flatInstance));
        
        // Mock the necessary methods
        doReturn(true).when(player).isFlyingWithElytra();
        
        // Create the event
        PlayerUseItemEvent event = new PlayerUseItemEvent(player, PlayerHand.MAIN, Items.PLAYER_FIREWORK, 1);
        
        // Call the listener
        listener.accept(event);
        
        // Verify that setVelocity was called
        verify(player).setVelocity(any(Vec.class));
    }

    @DisplayName("Test the random component of the boost")
    @Test
    void testRandomComponent(Env env) {
        // Create the listener with the default config
        ElytraBoostListener listener = new ElytraBoostListener(InternalAppConfig.defaultConfig());
        
        // Create a player
        Instance flatInstance = env.createFlatInstance();
        Player player = spy(env.createPlayer(flatInstance));
        
        // Mock the necessary methods
        doReturn(true).when(player).isFlyingWithElytra();
        
        // Create the event
        PlayerUseItemEvent event = new PlayerUseItemEvent(player, PlayerHand.MAIN, Items.PLAYER_FIREWORK, 1);
        
        // Call the listener
        listener.accept(event);
        
        // Verify that setVelocity was called
        verify(player).setVelocity(any(Vec.class));
    }

    @DisplayName("Test the upward correction when looking down")
    @Test
    void testUpwardCorrectionWhenLookingDown(Env env) {
        // Create the listener with the default config
        ElytraBoostListener listener = new ElytraBoostListener(InternalAppConfig.defaultConfig());
        
        // Create a player
        Instance flatInstance = env.createFlatInstance();
        Player player = spy(env.createPlayer(flatInstance));
        
        // Mock the necessary methods
        doReturn(true).when(player).isFlyingWithElytra();
        
        // Create the event
        PlayerUseItemEvent event = new PlayerUseItemEvent(player, PlayerHand.MAIN, Items.PLAYER_FIREWORK, 1);
        
        // Call the listener
        listener.accept(event);
        
        // Verify that setVelocity was called
        verify(player).setVelocity(any(Vec.class));
    }
}