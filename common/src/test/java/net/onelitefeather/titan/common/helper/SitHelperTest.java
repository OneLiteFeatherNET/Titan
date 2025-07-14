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
package net.onelitefeather.titan.common.helper;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.utils.Tags;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.UUID;

@ExtendWith(MicrotusExtension.class)
class SitHelperTest {

    @DisplayName("Test if player can sit at a specific location")
    @Test
    void testSitPlayer(Env env) {
        // Create a real instance and player
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        
        // Set player position
        Pos playerPos = new Pos(0, 64, 0);
        player.teleport(playerPos);
        
        // Create a custom AppConfig with a specific sit offset
        Vec sitOffset = new Vec(0, 0.3, 0);
        AppConfig customConfig = AppConfig.builder()
                .sitOffset(sitOffset)
                .build();
        
        // Make the player sit
        Pos sitLocation = new Pos(0, 64, 0);
        SitHelper.sitPlayer(player, sitLocation, customConfig);
        
        // Verify that the player is sitting
        Assertions.assertTrue(SitHelper.isSitting(player), "Player should be sitting");
        
        // Verify that the player has the SIT_PLAYER tag with the original position
        Assertions.assertTrue(player.hasTag(Tags.SIT_PLAYER), "Player should have SIT_PLAYER tag");
        Assertions.assertEquals(playerPos, player.getTag(Tags.SIT_PLAYER), "SIT_PLAYER tag should contain the original position");
        
        // Verify that the player has the SIT_ARROW tag
        Assertions.assertTrue(player.hasTag(Tags.SIT_ARROW), "Player should have SIT_ARROW tag");
        
        // Verify that the arrow entity exists in the instance
        UUID arrowUuid = player.getTag(Tags.SIT_ARROW);
        Assertions.assertNotNull(instance.getEntityByUuid(arrowUuid), "Arrow entity should exist in the instance");
    }
    
    @DisplayName("Test if player can be removed from sitting position")
    @Test
    void testRemovePlayer(Env env) {
        // Create a real instance and player
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        
        // Set player position
        Pos playerPos = new Pos(0, 64, 0);
        player.teleport(playerPos);
        
        // Create a custom AppConfig with a specific sit offset
        Vec sitOffset = new Vec(0, 0.3, 0);
        AppConfig customConfig = AppConfig.builder()
                .sitOffset(sitOffset)
                .build();
        
        // Make the player sit
        Pos sitLocation = new Pos(0, 64, 0);
        SitHelper.sitPlayer(player, sitLocation, customConfig);
        
        // Verify that the player is sitting
        Assertions.assertTrue(SitHelper.isSitting(player), "Player should be sitting");
        
        // Get the arrow UUID before removing the player
        UUID arrowUuid = player.getTag(Tags.SIT_ARROW);
        
        // Remove the player from the sitting position
        SitHelper.removePlayer(player);
        
        // Verify that the player is no longer sitting
        Assertions.assertFalse(SitHelper.isSitting(player), "Player should not be sitting");
        
        // Verify that the player no longer has the SIT_ARROW tag
        Assertions.assertFalse(player.hasTag(Tags.SIT_ARROW), "Player should not have SIT_ARROW tag");
        
        // Verify that the arrow entity no longer exists in the instance
        Assertions.assertNull(instance.getEntityByUuid(arrowUuid), "Arrow entity should not exist in the instance");
    }
    
    @DisplayName("Test if isSitting correctly identifies if a player is sitting")
    @Test
    void testIsSitting(Env env) {
        // Create a real instance and player
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        
        // Initially, the player should not be sitting
        Assertions.assertFalse(SitHelper.isSitting(player), "Player should not be sitting initially");
        
        // Set player position
        Pos playerPos = new Pos(0, 64, 0);
        player.teleport(playerPos);
        
        // Create a custom AppConfig with a specific sit offset
        Vec sitOffset = new Vec(0, 0.3, 0);
        AppConfig customConfig = AppConfig.builder()
                .sitOffset(sitOffset)
                .build();
        
        // Make the player sit
        Pos sitLocation = new Pos(0, 64, 0);
        SitHelper.sitPlayer(player, sitLocation, customConfig);
        
        // Now the player should be sitting
        Assertions.assertTrue(SitHelper.isSitting(player), "Player should be sitting after sitPlayer");
        
        // Remove the player from the sitting position
        SitHelper.removePlayer(player);
        
        // The player should no longer be sitting
        Assertions.assertFalse(SitHelper.isSitting(player), "Player should not be sitting after removePlayer");
    }
    
    @DisplayName("Test edge case: trying to sit a player who is already sitting")
    @Test
    void testSitPlayerAlreadySitting(Env env) {
        // Create a real instance and player
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        
        // Set player position
        Pos playerPos = new Pos(0, 64, 0);
        player.teleport(playerPos);
        
        // Create a custom AppConfig with a specific sit offset
        Vec sitOffset = new Vec(0, 0.3, 0);
        AppConfig customConfig = AppConfig.builder()
                .sitOffset(sitOffset)
                .build();
        
        // Make the player sit
        Pos sitLocation = new Pos(0, 64, 0);
        SitHelper.sitPlayer(player, sitLocation, customConfig);
        
        // Get the arrow UUID after the first sit
        UUID firstArrowUuid = player.getTag(Tags.SIT_ARROW);
        
        // Try to make the player sit again at a different location
        Pos newSitLocation = new Pos(1, 64, 1);
        SitHelper.sitPlayer(player, newSitLocation, customConfig);
        
        // Get the arrow UUID after the second sit
        UUID secondArrowUuid = player.getTag(Tags.SIT_ARROW);
        
        // Verify that the player is still sitting
        Assertions.assertTrue(SitHelper.isSitting(player), "Player should still be sitting");
        
        // Verify that a new arrow entity was created (the UUIDs should be different)
        Assertions.assertNotEquals(firstArrowUuid, secondArrowUuid, "A new arrow entity should have been created");
        
        // Verify that the first arrow entity no longer exists in the instance
        Assertions.assertNull(instance.getEntityByUuid(firstArrowUuid), "First arrow entity should not exist in the instance");
        
        // Verify that the second arrow entity exists in the instance
        Assertions.assertNotNull(instance.getEntityByUuid(secondArrowUuid), "Second arrow entity should exist in the instance");
    }
    
    @DisplayName("Test edge case: trying to remove a player who is not sitting")
    @Test
    void testRemovePlayerNotSitting(Env env) {
        // Create a real instance and player
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        
        // Verify that the player is not sitting
        Assertions.assertFalse(SitHelper.isSitting(player), "Player should not be sitting");
        
        // Try to remove the player from the sitting position
        SitHelper.removePlayer(player);
        
        // Verify that the player is still not sitting
        Assertions.assertFalse(SitHelper.isSitting(player), "Player should still not be sitting");
    }
    
    @DisplayName("Test edge case: trying to sit a player with a null instance")
    @Disabled
    @Test
    void testSitPlayerNullInstance(Env env) {
        // Create a player without an instance
        Player player = env.createPlayer(null);
        
        // Create a custom AppConfig with a specific sit offset
        Vec sitOffset = new Vec(0, 0.3, 0);
        AppConfig customConfig = AppConfig.builder()
                .sitOffset(sitOffset)
                .build();
        
        // Try to make the player sit
        Pos sitLocation = new Pos(0, 64, 0);
        SitHelper.sitPlayer(player, sitLocation, customConfig);
        
        // Verify that the player is not sitting
        Assertions.assertFalse(SitHelper.isSitting(player), "Player should not be sitting");
    }
}