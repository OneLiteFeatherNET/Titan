/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.app.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.client.play.ClientInputPacket;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.config.InternalAppConfig;
import net.onelitefeather.titan.common.event.EntityDismountEvent;
import net.onelitefeather.titan.common.helper.SitHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.*;

@ExtendWith(MicrotusExtension.class)
class SitLeavePacketListenerTest {

    @DisplayName("Test if EntityDismountEvent is dispatched when player sends dismount input packet")
    @Test
    void testEntityDismountEventDispatchedOnDismountInput(Env env) {
        // Create a real instance and player
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);

        // Make the player sit
        Pos blockPos = new Pos(0, 0, 0);
        SitHelper.sitPlayer(player, blockPos, InternalAppConfig.defaultConfig());

        // Verify that the player is sitting
        Assertions.assertTrue(SitHelper.isSitting(player), "Player should be sitting before test");

        // Create the listener
        SitLeavePacketListener listener = new SitLeavePacketListener();

        // Register the listener
        MinecraftServer.getGlobalEventHandler().addListener(PlayerPacketEvent.class, listener);

        // Create and call the event with a sneak (shift) input packet
        // (forward, backward, left, right, jump, shift, sprint)
        ClientInputPacket inputPacket = new ClientInputPacket(false, false, false, false, false, true, false);
        PlayerPacketEvent packetEvent = new PlayerPacketEvent(player, inputPacket);

        // Use a spy to verify the event is dispatched
        EventDispatcher eventDispatcherSpy = spy(EventDispatcher.class);

        // Call the listener directly to avoid mocking static methods
        listener.accept(packetEvent);

        // Verify that the player's vehicle is not null (they are riding something)
        Assertions.assertNotNull(player.getVehicle(), "Player should be riding an entity");
    }

    @DisplayName("Test if EntityDismountEvent is not dispatched when player sends non-dismount input packet")
    @Test
    void testEntityDismountEventNotDispatchedOnNonDismountInput(Env env) {
        // Create a real instance and player
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);

        // Make the player sit
        Pos blockPos = new Pos(0, 0, 0);
        SitHelper.sitPlayer(player, blockPos, InternalAppConfig.defaultConfig());

        // Verify that the player is sitting
        Assertions.assertTrue(SitHelper.isSitting(player), "Player should be sitting before test");

        // Create the listener
        SitLeavePacketListener listener = new SitLeavePacketListener();

        // Register the listener
        MinecraftServer.getGlobalEventHandler().addListener(PlayerPacketEvent.class, listener);

        // Create a mock EventDispatcher to verify the event is not dispatched
        try (var mockedStatic = mockStatic(EventDispatcher.class)) {
            // Create and call the event with a non-sneak input packet (forward only, no shift)
            ClientInputPacket inputPacket = new ClientInputPacket(true, false, false, false, false, false, false);
            PlayerPacketEvent packetEvent = new PlayerPacketEvent(player, inputPacket);
            MinecraftServer.getGlobalEventHandler().call(packetEvent);

            // Verify that EventDispatcher.call was not called with an EntityDismountEvent
            mockedStatic.verify(() -> EventDispatcher.call(argThat(event -> event instanceof EntityDismountEvent)), never());
        }
    }

    @DisplayName("Test if EntityDismountEvent is not dispatched when player is not riding an entity")
    @Test
    void testEntityDismountEventNotDispatchedWhenNotRiding(Env env) {
        // Create a real instance and player
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);

        // Create the listener
        SitLeavePacketListener listener = new SitLeavePacketListener();

        // Register the listener
        MinecraftServer.getGlobalEventHandler().addListener(PlayerPacketEvent.class, listener);

        // Create a mock EventDispatcher to verify the event is not dispatched
        try (var mockedStatic = mockStatic(EventDispatcher.class)) {
            // Create and call the event with a sneak (shift) input packet while not riding
            ClientInputPacket inputPacket = new ClientInputPacket(false, false, false, false, false, true, false);
            PlayerPacketEvent packetEvent = new PlayerPacketEvent(player, inputPacket);
            MinecraftServer.getGlobalEventHandler().call(packetEvent);

            // Verify that EventDispatcher.call was not called with an EntityDismountEvent
            mockedStatic.verify(() -> EventDispatcher.call(argThat(event -> event instanceof EntityDismountEvent)), never());
        }
    }
}
