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
package net.onelitefeather.titan.common.config;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigIntegrationTest {

    @Test
    @DisplayName("Test full configuration lifecycle")
    void testFullConfigurationLifecycle() {
        // Create initial configuration
        AppConfig initialConfig = AppConfig.builder().tickleDuration(1000L).sitOffset(new Vec(0, 1.5, 0)).allowedSitBlocks(new ArrayList<>()).simulationDistance(10).fireworkBoostSlot(8).elytraBoostMultiplier(1.5).minHeightBeforeTeleport(-64).maxHeightBeforeTeleport(320).build();

        // Verify initial configuration
        assertEquals(1000L, initialConfig.tickleDuration());
        assertEquals(new Vec(0, 1.5, 0), initialConfig.sitOffset());
        assertTrue(initialConfig.allowedSitBlocks().isEmpty());
        assertEquals(10, initialConfig.simulationDistance());
        assertEquals(8, initialConfig.fireworkBoostSlot());
        assertEquals(1.5, initialConfig.elytraBoostMultiplier());
        assertEquals(-64, initialConfig.minHeightBeforeTeleport());
        assertEquals(320, initialConfig.maxHeightBeforeTeleport());

        // Modify configuration by adding sit blocks
        AppConfig configWithBlocks = AppConfig.builder(initialConfig).addAllowedSitBlock(Material.OAK_STAIRS).addAllowedSitBlock(Material.STONE_STAIRS).build();

        // Verify modified configuration
        assertEquals(initialConfig.tickleDuration(), configWithBlocks.tickleDuration());
        assertEquals(initialConfig.sitOffset(), configWithBlocks.sitOffset());
        assertEquals(2, configWithBlocks.allowedSitBlocks().size());
        assertTrue(configWithBlocks.allowedSitBlocks().contains(Key.key("minecraft:oak_stairs")));
        assertTrue(configWithBlocks.allowedSitBlocks().contains(Key.key("minecraft:stone_stairs")));

        // Modify configuration by removing a sit block
        AppConfig configWithOneBlock = AppConfig.builder(configWithBlocks).removeAllowedSitBlock(Material.STONE_STAIRS).build();

        // Verify modified configuration
        assertEquals(1, configWithOneBlock.allowedSitBlocks().size());
        assertTrue(configWithOneBlock.allowedSitBlocks().contains(Key.key("minecraft:oak_stairs")));
        assertFalse(configWithOneBlock.allowedSitBlocks().contains(Key.key("minecraft:stone_stairs")));

        // Verify displayConfig returns a non-null Component
        Component displayComponent = configWithOneBlock.displayConfig();
        assertNotNull(displayComponent);
    }

    @Test
    @DisplayName("Test configuration with multiple changes")
    void testConfigurationWithMultipleChanges() {
        // Create initial configuration
        AppConfig initialConfig = AppConfig.builder().tickleDuration(1000L).sitOffset(new Vec(0, 1.5, 0)).allowedSitBlocks(List.of(Key.key("minecraft:oak_stairs"))).simulationDistance(10).fireworkBoostSlot(8).elytraBoostMultiplier(1.5).minHeightBeforeTeleport(-64).maxHeightBeforeTeleport(320).build();

        // Make multiple changes
        AppConfig modifiedConfig = AppConfig.builder(initialConfig).tickleDuration(2000L).sitOffset(new Vec(0, 2.0, 0)).simulationDistance(15).fireworkBoostSlot(9).elytraBoostMultiplier(2.0).minHeightBeforeTeleport(-128).maxHeightBeforeTeleport(384).build();

        // Verify all changes were applied
        assertEquals(2000L, modifiedConfig.tickleDuration());
        assertEquals(new Vec(0, 2.0, 0), modifiedConfig.sitOffset());
        assertEquals(15, modifiedConfig.simulationDistance());
        assertEquals(9, modifiedConfig.fireworkBoostSlot());
        assertEquals(2.0, modifiedConfig.elytraBoostMultiplier());
        assertEquals(-128, modifiedConfig.minHeightBeforeTeleport());
        assertEquals(384, modifiedConfig.maxHeightBeforeTeleport());

        // Verify that the allowed sit blocks were preserved
        assertEquals(1, modifiedConfig.allowedSitBlocks().size());
        assertTrue(modifiedConfig.allowedSitBlocks().contains(Key.key("minecraft:oak_stairs")));
    }
}