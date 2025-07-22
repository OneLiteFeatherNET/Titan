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

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.utils.Tags;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MicrotusExtension.class)
class TickleListenerTest {

	@DisplayName("Test if tickle message is sent and cooldown is applied when player with feather attacks another player")
	@Test
	void testTickleMessageSentAndCooldownApplied(Env env) {
		// Create a real instance and players
		Instance flatInstance = spy(env.createFlatInstance());
		Player attacker = spy(env.createPlayer(flatInstance));
		Player target = spy(env.createPlayer(flatInstance));

		// Create a custom AppConfig with a specific tickle duration
		long tickleDuration = 2000L; // 2 seconds
		AppConfig customConfig = AppConfig.builder().tickleDuration(tickleDuration).build();

		// Create the listener with our test configuration
		TickleListener listener = new TickleListener(customConfig);

		// Register the listener
		MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent.class, listener);

		// Mock the player connection to verify packet sending
		PlayerConnection playerConnection = mock(PlayerConnection.class);
		when(attacker.getPlayerConnection()).thenReturn(playerConnection);

		// Give the attacker a feather in the offhand
		when(attacker.getItemInOffHand()).thenReturn(ItemStack.of(Material.FEATHER));
		when(attacker.getItemInMainHand()).thenReturn(ItemStack.AIR);

		// Mock the instance players to verify message sending
		Set<Player> instancePlayers = new HashSet<>();
		instancePlayers.add(attacker);
		instancePlayers.add(target);
		doReturn(instancePlayers).when(flatInstance).getPlayers();

		// Create and call the event
		EntityAttackEvent attackEvent = new EntityAttackEvent(attacker, target);
		MinecraftServer.getGlobalEventHandler().call(attackEvent);

		// Verify that the cooldown tag was set
		Assertions.assertTrue(attacker.hasTag(Tags.TICKLE_COOLDOWN), "Attacker should have cooldown tag");

		// Verify that a cooldown packet was sent
		verify(playerConnection, times(1)).sendPacket(any(SetCooldownPacket.class));

		// Verify that messages were sent to all players in the instance
		verify(attacker, times(1)).sendMessage(any(Component.class));
		verify(target, times(1)).sendMessage(any(Component.class));
	}

	@DisplayName("Test if tickle is not applied when player doesn't have a feather")
	@Test
	void testTickleNotAppliedWithoutFeather(Env env) {
		// Create a real instance and players
		Instance flatInstance = env.createFlatInstance();
		Player attacker = spy(env.createPlayer(flatInstance));
		Player target = spy(env.createPlayer(flatInstance));

		// Create a custom AppConfig with a specific tickle duration
		long tickleDuration = 2000L; // 2 seconds
		AppConfig customConfig = AppConfig.builder().tickleDuration(tickleDuration).build();

		// Create the listener with our test configuration
		TickleListener listener = new TickleListener(customConfig);

		// Register the listener
		MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent.class, listener);

		// Mock the player connection to verify no packet is sent
		PlayerConnection playerConnection = mock(PlayerConnection.class);
		when(attacker.getPlayerConnection()).thenReturn(playerConnection);

		// Give the attacker no feather
		when(attacker.getItemInOffHand()).thenReturn(ItemStack.AIR);
		when(attacker.getItemInMainHand()).thenReturn(ItemStack.AIR);

		// Create and call the event
		EntityAttackEvent attackEvent = new EntityAttackEvent(attacker, target);
		MinecraftServer.getGlobalEventHandler().call(attackEvent);

		// Verify that the cooldown tag was not set
		Assertions.assertFalse(attacker.hasTag(Tags.TICKLE_COOLDOWN), "Attacker should not have cooldown tag");

		// Verify that no cooldown packet was sent
		verify(playerConnection, never()).sendPacket(any(SetCooldownPacket.class));

		// Verify that no messages were sent
		verify(attacker, never()).sendMessage(any(Component.class));
		verify(target, never()).sendMessage(any(Component.class));
	}

	@DisplayName("Test if tickle is not applied when player is on cooldown")
	@Test
	void testTickleNotAppliedWhenOnCooldown(Env env) {
		// Create a real instance and players
		Instance flatInstance = spy(env.createFlatInstance());
		Player attacker = spy(env.createPlayer(flatInstance));
		Player target = spy(env.createPlayer(flatInstance));

		// Create a custom AppConfig with a specific tickle duration
		long tickleDuration = 2000L; // 2 seconds
		AppConfig customConfig = AppConfig.builder().tickleDuration(tickleDuration).build();

		// Create the listener with our test configuration
		TickleListener listener = new TickleListener(customConfig);

		// Register the listener
		MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent.class, listener);

		// Mock the player connection to verify packet sending
		PlayerConnection playerConnection = mock(PlayerConnection.class);
		when(attacker.getPlayerConnection()).thenReturn(playerConnection);

		// Give the attacker a feather
		when(attacker.getItemInOffHand()).thenReturn(ItemStack.of(Material.FEATHER));

		// Set the cooldown tag (future time)
		long futureTime = System.currentTimeMillis() + 1000;
		attacker.setTag(Tags.TICKLE_COOLDOWN, futureTime);

		// Mock the instance players to verify message sending
		Set<Player> instancePlayers = new HashSet<>();
		instancePlayers.add(attacker);
		instancePlayers.add(target);
		doReturn(instancePlayers).when(flatInstance).getPlayers();

		// Create and call the event
		EntityAttackEvent attackEvent = new EntityAttackEvent(attacker, target);
		MinecraftServer.getGlobalEventHandler().call(attackEvent);

		// Verify that the cooldown tag still exists
		Assertions.assertTrue(attacker.hasTag(Tags.TICKLE_COOLDOWN), "Attacker should still have cooldown tag");

		// Verify that no new cooldown packet was sent
		verify(playerConnection, never()).sendPacket(any(SetCooldownPacket.class));

		// Verify that no messages were sent
		verify(attacker, never()).sendMessage(any(Component.class));
		verify(target, never()).sendMessage(any(Component.class));
	}
}
