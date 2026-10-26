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
package net.onelitefeather.titan.feature.navigator;

import net.minestom.server.item.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NavigatorLayoutTest {

    @DisplayName("The navigator has four rows of nine slots")
    @Test
    void fourRowsOfNine() {
        Assertions.assertEquals(36, NavigatorLayout.SIZE, "four rows of nine slots");
    }

    @DisplayName("The frame is the top and bottom row and the first and last column")
    @Test
    void frameCoversTheOuterSlots() {
        for (int slot : new int[]{0, 4, 8, 9, 17, 18, 26, 27, 31, 35}) {
            Assertions.assertTrue(NavigatorLayout.isFrame(slot), "slot " + slot + " belongs to the frame");
        }
        for (int slot : new int[]{10, 11, 13, 15, 16, 19, 20, 22, 24, 25}) {
            Assertions.assertFalse(NavigatorLayout.isFrame(slot), "slot " + slot + " is inside the frame");
        }
    }

    @DisplayName("Frame panes alternate between light blue and cyan by row plus column")
    @Test
    void frameAlternatesByParity() {
        Assertions.assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, NavigatorLayout.frameMaterial(0), "the top-left corner is light blue");
        Assertions.assertEquals(Material.CYAN_STAINED_GLASS_PANE, NavigatorLayout.frameMaterial(1), "the next pane is cyan");
        Assertions.assertEquals(Material.CYAN_STAINED_GLASS_PANE, NavigatorLayout.frameMaterial(9), "the pane below the corner is cyan");
        Assertions.assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, NavigatorLayout.frameMaterial(8), "the top-right corner is light blue");
        Assertions.assertEquals(Material.CYAN_STAINED_GLASS_PANE, NavigatorLayout.frameMaterial(27), "the bottom-left corner is cyan, one row down the parity flips");
    }

    @Disabled("navigator-staging-on-demand wave 2: layout")
    @DisplayName("Minigames sit in row one and servers in row two")
    @Test
    void destinationsSitOnTheirRows() {
        Assertions.assertEquals(11, Destination.ELYTRA_RACE.slot(), "ElytraRace is in row one");
        Assertions.assertEquals(13, Destination.BOUNCE.slot(), "Bounce is in row one, between ElytraRace and Slender");
        Assertions.assertEquals(15, Destination.SLENDER.slot(), "Slender is in row one");
        Assertions.assertEquals(16, Destination.SLENDER_STAGING.slot(), "Slender (Staging) is in row one, right after Slender");
        Assertions.assertEquals(20, Destination.SURVIVAL.slot(), "Survival is in row two");
        Assertions.assertEquals(22, Destination.CREATIVE.slot(), "Creative is in row two");
        Assertions.assertEquals(24, Destination.BUILD.slot(), "Build is in row two");
    }

    @DisplayName("Slender (Staging) takes slot 16, which is inside the frame and used by no other destination")
    @Test
    void slenderStagingTakesTheFreeSlotNextToSlender() {
        Assertions.assertFalse(NavigatorLayout.isFrame(16), "slot 16 is inside the frame");
        for (Destination destination : Destination.values()) {
            if (destination != Destination.SLENDER_STAGING) {
                Assertions.assertNotEquals(16, destination.slot(), destination + " must not share slot 16 with Slender (Staging)");
            }
        }
    }

    @Disabled("navigator-staging-on-demand wave 2: layout")
    @DisplayName("No destination sits on the frame")
    @Test
    void destinationsAreInside() {
        for (Destination destination : Destination.values()) {
            Assertions.assertFalse(NavigatorLayout.isFrame(destination.slot()), destination + " must not sit on the frame");
        }
    }

    @DisplayName("Spawn sits in the bottom centre and replaces the frame pane there")
    @Test
    void spawnIsBottomCentre() {
        Assertions.assertEquals(31, NavigatorLayout.SPAWN_SLOT, "spawn is in the middle of the bottom row");
        Assertions.assertTrue(NavigatorLayout.isFrame(NavigatorLayout.SPAWN_SLOT), "the bottom row is the frame, spawn takes its middle");
    }
}
