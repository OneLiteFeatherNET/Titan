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
package net.onelitefeather.titan.common.config;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigImplTest {

    @Test
    @DisplayName("Test AppConfigImpl constructor and getters")
    void testConstructorAndGetters() {
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
        AppConfigImpl appConfig = new AppConfigImpl(
                tickleDuration,
                sitOffset,
                allowedSitBlocks,
                simulationDistance,
                fireworkBoostSlot,
                elytraBoostMultiplier,
                minHeightBeforeTeleport,
                maxHeightBeforeTeleport
        );

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
    @DisplayName("Test displayConfig method returns non-null Component")
    void testDisplayConfig() {
        // Arrange
        AppConfigImpl appConfig = new AppConfigImpl(
                1000L,
                new Vec(0, 1.5, 0),
                List.of(Key.key("minecraft:oak_stairs")),
                10,
                8,
                1.5,
                -64,
                320
        );

        // Act
        Component displayComponent = appConfig.displayConfig();

        // Assert
        assertNotNull(displayComponent);
    }

    @Test
    @DisplayName("Test equals and hashCode methods")
    void testEqualsAndHashCode() {
        // Arrange
        AppConfigImpl appConfig1 = new AppConfigImpl(
                1000L,
                new Vec(0, 1.5, 0),
                List.of(Key.key("minecraft:oak_stairs")),
                10,
                8,
                1.5,
                -64,
                320
        );

        AppConfigImpl appConfig2 = new AppConfigImpl(
                1000L,
                new Vec(0, 1.5, 0),
                List.of(Key.key("minecraft:oak_stairs")),
                10,
                8,
                1.5,
                -64,
                320
        );

        AppConfigImpl appConfig3 = new AppConfigImpl(
                2000L, // Different value
                new Vec(0, 1.5, 0),
                List.of(Key.key("minecraft:oak_stairs")),
                10,
                8,
                1.5,
                -64,
                320
        );

        // Assert
        assertEquals(appConfig1, appConfig2);
        assertEquals(appConfig1.hashCode(), appConfig2.hashCode());
        
        assertNotEquals(appConfig1, appConfig3);
        assertNotEquals(appConfig1.hashCode(), appConfig3.hashCode());
    }
}