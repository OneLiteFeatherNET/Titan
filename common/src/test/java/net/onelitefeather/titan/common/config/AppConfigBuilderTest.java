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
package net.onelitefeather.titan.common.config;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigBuilderTest {

    @Test
    @DisplayName("Test builder creates AppConfig with correct values")
    void testBuilderCreatesAppConfigWithCorrectValues() {
        // Arrange
        long tickleDuration = 1000L;
        Vec sitOffset = new Vec(0, 1.5, 0);
        List<Key> allowedSitBlocks = new ArrayList<>();
        allowedSitBlocks.add(Key.key("minecraft:oak_stairs"));
        int simulationDistance = 10;
        int fireworkBoostSlot = 8;
        double elytraBoostMultiplier = 1.5;
        int minHeightBeforeTeleport = -64;
        int maxHeightBeforeTeleport = 320;

        // Act
        AppConfig appConfig = AppConfig.builder().tickleDuration(tickleDuration).sitOffset(sitOffset).allowedSitBlocks(allowedSitBlocks).simulationDistance(simulationDistance).fireworkBoostSlot(fireworkBoostSlot).elytraBoostMultiplier(elytraBoostMultiplier).minHeightBeforeTeleport(minHeightBeforeTeleport).maxHeightBeforeTeleport(maxHeightBeforeTeleport).build();

        // Assert
        assertEquals(tickleDuration, appConfig.tickleDuration());
        assertEquals(sitOffset, appConfig.sitOffset());
        assertEquals(allowedSitBlocks, appConfig.allowedSitBlocks());
        assertEquals(simulationDistance, appConfig.simulationDistance());
        assertEquals(fireworkBoostSlot, appConfig.fireworkBoostSlot());
        assertEquals(elytraBoostMultiplier, appConfig.elytraBoostMultiplier());
        assertEquals(minHeightBeforeTeleport, appConfig.minHeightBeforeTeleport());
        assertEquals(maxHeightBeforeTeleport, appConfig.maxHeightBeforeTeleport());
    }

    @Test
    @DisplayName("Test builder with existing AppConfig")
    void testBuilderWithExistingAppConfig() {
        // Arrange
        AppConfig originalConfig = AppConfig.builder().tickleDuration(1000L).sitOffset(new Vec(0, 1.5, 0)).allowedSitBlocks(List.of(Key.key("minecraft:oak_stairs"))).simulationDistance(10).fireworkBoostSlot(8).elytraBoostMultiplier(1.5).minHeightBeforeTeleport(-64).maxHeightBeforeTeleport(320).build();

        // Act
        AppConfig newConfig = AppConfig.builder(originalConfig).tickleDuration(2000L) // Change one value
                .build();

        // Assert
        assertEquals(2000L, newConfig.tickleDuration());
        assertEquals(originalConfig.sitOffset(), newConfig.sitOffset());
        assertEquals(originalConfig.allowedSitBlocks(), newConfig.allowedSitBlocks());
        assertEquals(originalConfig.simulationDistance(), newConfig.simulationDistance());
        assertEquals(originalConfig.fireworkBoostSlot(), newConfig.fireworkBoostSlot());
        assertEquals(originalConfig.elytraBoostMultiplier(), newConfig.elytraBoostMultiplier());
    }

    @Test
    @DisplayName("Test add and remove allowed sit blocks")
    void testAddAndRemoveAllowedSitBlocks() {
        // Arrange
        List<Key> initialBlocks = new ArrayList<>();
        initialBlocks.add(Key.key("minecraft:oak_stairs"));

        Key stoneStairsKey = Key.key("minecraft:stone_stairs");
        Key brickStairsKey = Key.key("minecraft:brick_stairs");

        // Act
        AppConfig appConfig = AppConfig.builder().allowedSitBlocks(initialBlocks).addAllowedSitBlock(stoneStairsKey).addAllowedSitBlock(Material.BRICK_STAIRS) // Test the Material overload
                .build();

        // Assert - Both blocks should be added
        List<Key> allowedBlocks = appConfig.allowedSitBlocks();
        assertTrue(allowedBlocks.contains(Key.key("minecraft:oak_stairs")));
        assertTrue(allowedBlocks.contains(stoneStairsKey));
        assertTrue(allowedBlocks.contains(brickStairsKey));
        assertEquals(3, allowedBlocks.size());

        // Act - Remove a block
        AppConfig updatedConfig = AppConfig.builder(appConfig).removeAllowedSitBlock(stoneStairsKey).removeAllowedSitBlock(Material.BRICK_STAIRS) // Test the Material overload
                .build();

        // Assert - Blocks should be removed
        List<Key> updatedBlocks = updatedConfig.allowedSitBlocks();
        assertTrue(updatedBlocks.contains(Key.key("minecraft:oak_stairs")));
        assertFalse(updatedBlocks.contains(stoneStairsKey));
        assertFalse(updatedBlocks.contains(brickStairsKey));
        assertEquals(1, updatedBlocks.size());
    }
}