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
package net.onelitefeather.titan.app.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.listener.UseItemListener;
import net.minestom.server.network.packet.client.play.ClientUseItemPacket;
import net.minestom.testing.Collector;
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
        AppConfig customConfig = AppConfig.builder().elytraBoostMultiplier(boostMultiplier).build();

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

        // Get the captured velocity (the first vanilla boost tick, applied synchronously)
        Vec capturedVelocity = velocityCaptor.getValue();

        // The first-tick boost is deterministic (randomness only affects the lifetime).
        Vec expected = expectedFirstTick(initialVelocity, playerPos.direction(), boostMultiplier);
        double tolerance = 1.0E-6;

        assertTrue(Math.abs(capturedVelocity.x() - expected.x()) <= tolerance, "X component of velocity should match the vanilla first-tick boost");
        assertTrue(Math.abs(capturedVelocity.y() - expected.y()) <= tolerance, "Y component of velocity should match the vanilla first-tick boost");
        assertTrue(Math.abs(capturedVelocity.z() - expected.z()) <= tolerance, "Z component of velocity should match the vanilla first-tick boost");
    }

    /**
     * Replicates the first-tick velocity the listener applies: the vanilla per-tick formula
     * {@code vel += look*0.1 + (look*1.5 - vel)*0.5} seeded from the current velocity, converted
     * back to Minestom's per-second velocity and scaled by the boost multiplier.
     */
    private static Vec expectedFirstTick(Vec currentVelocity, Vec look, double multiplier) {
        double tps = ServerFlag.SERVER_TICKS_PER_SECOND;
        Vec velocity = currentVelocity.div(tps);
        Vec next = velocity.add(
                look.x() * 0.1 + (look.x() * 1.5 - velocity.x()) * 0.5, look.y() * 0.1 + (look.y() * 1.5 - velocity.y()) * 0.5, look.z() * 0.1 + (look.z() * 1.5 - velocity.z()) * 0.5);
        return next.mul(tps * multiplier);
    }

    @DisplayName("Test the boost applies a velocity with the default config")
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

    @DisplayName("Test the boost accelerates along the look direction when looking down")
    @Test
    void testUpwardCorrectionWhenLookingDown(Env env) {
        // Create a custom AppConfig with a specific boost multiplier
        double boostMultiplier = 2.0;
        AppConfig customConfig = AppConfig.builder().elytraBoostMultiplier(boostMultiplier).build();

        // Create the listener with the custom config
        ElytraBoostListener listener = new ElytraBoostListener(customConfig);
        EventListener<PlayerUseItemEvent> playerUseItemEventEventListener = EventListener.of(PlayerUseItemEvent.class, listener);
        env.process().eventHandler().addListener(playerUseItemEventEventListener);

        // Create a player
        Instance flatInstance = env.createFlatInstance();
        // Set up a position with the player looking down (pitch=90, yaw=0)
        Player player = env.createPlayer(flatInstance, new Pos(0, 64, 0, 90, 0));
        player.setItemInMainHand(Items.PLAYER_FIREWORK);
        Vec initialVelocity = new Vec(1.0, 0.0, 0.0);
        player.setVelocity(initialVelocity);
        player.setFlyingWithElytra(true);

        // Assert initial velocity is zero
        assertEquals(initialVelocity, player.getVelocity());
        Collector<PlayerUseItemEvent> playerUseItemEventCollector = env.trackEvent(PlayerUseItemEvent.class, EventFilter.PLAYER, player);

        // Create the event
        ClientUseItemPacket packet = new ClientUseItemPacket(PlayerHand.MAIN, 42, 0f, 0f);
        UseItemListener.useItemListener(packet, player);

        playerUseItemEventCollector.assertSingle();

        // The boost points along the look direction (here: straight down), with no special
        // "looking down" correction - just the vanilla first-tick acceleration.
        Vec expected = expectedFirstTick(initialVelocity, player.getPosition().direction(), boostMultiplier);
        Vec actual = player.getVelocity();
        double tolerance = 1.0E-3;

        assertTrue(Math.abs(actual.x() - expected.x()) <= tolerance);
        assertTrue(Math.abs(actual.y() - expected.y()) <= tolerance);
        assertTrue(Math.abs(actual.z() - expected.z()) <= tolerance);

        env.destroyInstance(flatInstance, true);
        env.process().eventHandler().removeListener(playerUseItemEventEventListener);
    }
}
