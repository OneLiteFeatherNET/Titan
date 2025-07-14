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
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.config.InternalAppConfig;
import net.onelitefeather.titan.common.map.LobbyMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class PlayerMoveListenerTest {

    @DisplayName("Test if player is teleported when below min height")
    @Test
    void testPlayerTeleportBelowMinHeight(Env env) {
        // Create a real instance and player
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);

        // Create a real AppConfig
        var appConfig = InternalAppConfig.defaultConfig();

        // Create a real LobbyMap with a spawn point
        var lobbyMap = LobbyMap.lobbyMapBuilder().build();
        Pos spawnPos = new Pos(10, 100, 10);
        lobbyMap.setSpawn(spawnPos);

        // Create the listener with real objects
        PlayerMoveListener listener = new PlayerMoveListener(appConfig, lobbyMap);

        // Register the listener
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, listener);

        // Move the player below min height
        Pos belowMinPos = new Pos(0, appConfig.minHeightBeforeTeleport() - 10, 0);
        player.teleport(belowMinPos);

        // Create and call the event
        PlayerMoveEvent moveEvent = new PlayerMoveEvent(player, belowMinPos, true);
        MinecraftServer.getGlobalEventHandler().call(moveEvent);

        // Verify that the player was teleported to the spawn position
        Assertions.assertEquals(spawnPos, player.getPosition());
    }

    @DisplayName("Test if player is teleported when above max height")
    @Test
    void testPlayerTeleportAboveMaxHeight(Env env) {
        // Create a real instance and player
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);

        // Create a real AppConfig
        var appConfig = InternalAppConfig.defaultConfig();

        // Create a real LobbyMap with a spawn point
        var lobbyMap = LobbyMap.lobbyMapBuilder().build();
        Pos spawnPos = new Pos(10, 100, 10);
        lobbyMap.setSpawn(spawnPos);

        // Create the listener with real objects
        PlayerMoveListener listener = new PlayerMoveListener(appConfig, lobbyMap);

        // Register the listener
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, listener);

        // Move the player above max height
        Pos aboveMaxPos = new Pos(0, appConfig.maxHeightBeforeTeleport() + 10, 0);
        player.teleport(aboveMaxPos);

        // Create and call the event
        PlayerMoveEvent moveEvent = new PlayerMoveEvent(player, aboveMaxPos, true);
        MinecraftServer.getGlobalEventHandler().call(moveEvent);

        // Verify that the player was teleported to the spawn position
        Assertions.assertEquals(spawnPos, player.getPosition());
    }

    @DisplayName("Test if player is not teleported when within height limits")
    @Test
    void testPlayerNotTeleportedWithinLimits(Env env) {
        // Create a real instance and player
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);

        // Create a real AppConfig
        var appConfig = InternalAppConfig.defaultConfig();

        // Create a real LobbyMap with a spawn point
        var lobbyMap = LobbyMap.lobbyMapBuilder().build();
        Pos spawnPos = new Pos(10, 100, 10);
        lobbyMap.setSpawn(spawnPos);

        // Create the listener with real objects
        PlayerMoveListener listener = new PlayerMoveListener(appConfig, lobbyMap);

        // Register the listener
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, listener);

        // Move the player to a position within the height limits
        Pos validPos = new Pos(0, (appConfig.minHeightBeforeTeleport() + appConfig.maxHeightBeforeTeleport()) / 2, 0);
        player.teleport(validPos);

        // Create and call the event
        PlayerMoveEvent moveEvent = new PlayerMoveEvent(player, validPos, true);
        MinecraftServer.getGlobalEventHandler().call(moveEvent);

        // Verify that the player was not teleported
        Assertions.assertEquals(validPos, player.getPosition());
    }
}
