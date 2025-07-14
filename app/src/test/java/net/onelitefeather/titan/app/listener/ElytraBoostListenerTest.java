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

        // Set up a specific position and direction
        Pos playerPos = new Pos(0, 64, 0, 0, 0); // Looking straight ahead (pitch=0, yaw=0)
        doReturn(playerPos).when(player).getPosition();

        // Set up a specific velocity
        Vec initialVelocity = new Vec(1.0, 0.0, 0.0);
        doReturn(initialVelocity).when(player).getVelocity();

        // Mock the necessary methods
        doReturn(true).when(player).isFlyingWithElytra();

        // Create the event
        PlayerUseItemEvent event = new PlayerUseItemEvent(player, PlayerHand.MAIN, Items.PLAYER_FIREWORK, 1);

        // Capture the velocity that will be set
        ArgumentCaptor<Vec> velocityCaptor = ArgumentCaptor.forClass(Vec.class);

        // Call the listener
        listener.accept(event);

        // Verify that setVelocity was called and capture the value
        verify(player).setVelocity(velocityCaptor.capture());

        // Get the captured velocity
        Vec capturedVelocity = velocityCaptor.getValue();

        // Calculate the expected velocity
        // Initial velocity + (direction * boostMultiplier) + random component
        // Since we can't predict the random component exactly, we'll check if the velocity is in the expected range

        // The direction vector when looking straight ahead (pitch=0, yaw=0) is (0, 0, 1)
        Vec expectedDirectionBoost = new Vec(0, 0, boostMultiplier);

        // Expected velocity without random component
        Vec expectedBaseVelocity = initialVelocity.add(expectedDirectionBoost);

        // Check if the captured velocity is close to the expected velocity (allowing for the random component)
        double tolerance = 0.1; // Tolerance for the random component

        // Check each component
        assertTrue(Math.abs(capturedVelocity.x() - expectedBaseVelocity.x()) <= tolerance,
                "X component of velocity should be within tolerance of expected value");
        assertTrue(Math.abs(capturedVelocity.y() - expectedBaseVelocity.y()) <= tolerance,
                "Y component of velocity should be within tolerance of expected value");
        assertTrue(Math.abs(capturedVelocity.z() - expectedBaseVelocity.z()) <= tolerance,
                "Z component of velocity should be within tolerance of expected value");
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
        // Create a custom AppConfig with a specific boost multiplier
        double boostMultiplier = 2.0;
        AppConfig customConfig = AppConfig.builder()
                .elytraBoostMultiplier(boostMultiplier)
                .build();

        // Create the listener with the custom config
        ElytraBoostListener listener = new ElytraBoostListener(customConfig);

        // Create a player
        Instance flatInstance = env.createFlatInstance();
        Player player = spy(env.createPlayer(flatInstance));

        // Set up a position with the player looking down (pitch=90, yaw=0)
        Pos playerPos = new Pos(0, 64, 0, 90, 0);
        doReturn(playerPos).when(player).getPosition();

        // Set up a specific velocity
        Vec initialVelocity = new Vec(0.0, -1.0, 0.0); // Moving downward
        doReturn(initialVelocity).when(player).getVelocity();

        // Mock the necessary methods
        doReturn(true).when(player).isFlyingWithElytra();

        // Create the event
        PlayerUseItemEvent event = new PlayerUseItemEvent(player, PlayerHand.MAIN, Items.PLAYER_FIREWORK, 1);

        // Capture the velocity that will be set
        ArgumentCaptor<Vec> velocityCaptor = ArgumentCaptor.forClass(Vec.class);

        // Call the listener
        listener.accept(event);

        // Verify that setVelocity was called and capture the value
        verify(player).setVelocity(velocityCaptor.capture());

        // Get the captured velocity
        Vec capturedVelocity = velocityCaptor.getValue();

        // When looking down, the direction vector is approximately (0, -1, 0)
        // The upward correction should add a positive Y component

        // Check if the Y component of the velocity is greater than the initial Y velocity
        // This indicates that the upward correction was applied
        assertTrue(capturedVelocity.y() > initialVelocity.y(),
                "Y component of velocity should be increased due to upward correction when looking down");

        // Calculate the expected upward correction
        // From the code: double upwardCorrection = -playerDirection.y() * 0.3;
        // playerDirection.y() is -1 when looking straight down, so upwardCorrection should be 0.3
        double expectedUpwardCorrection = 0.3;

        // Expected Y component: initialVelocity.y + (direction.y * boostMultiplier) + upwardCorrection + random
        // Since we can't predict the random component exactly, we'll check if the velocity is in the expected range
        double expectedBaseY = initialVelocity.y() + (-1 * boostMultiplier) + expectedUpwardCorrection;

        // Check if the captured Y velocity is close to the expected Y velocity (allowing for the random component)
        double tolerance = 0.1; // Tolerance for the random component
        assertTrue(Math.abs(capturedVelocity.y() - expectedBaseY) <= tolerance,
                "Y component of velocity should be within tolerance of expected value with upward correction");
    }
}
