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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NavigatorLayoutTest {

    private static final List<Destination> FOUR_MINIGAMES = List.of(Destination.ELYTRA_RACE, Destination.BOUNCE, Destination.TAMIAS, Destination.SLENDER);
    private static final List<Destination> THREE_SERVERS = List.of(Destination.SURVIVAL, Destination.CREATIVE, Destination.BUILD);
    private static final List<Destination> ALL_MAIN = List.of(Destination.ELYTRA_RACE, Destination.BOUNCE, Destination.TAMIAS, Destination.SLENDER, Destination.SURVIVAL, Destination.CREATIVE, Destination.BUILD);

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

    @DisplayName("The frame is one dark pane, not the light blue and cyan alternation")
    @Test
    void frameIsOneBlackPane() {
        Assertions.assertEquals(Material.BLACK_STAINED_GLASS_PANE, NavigatorLayout.FRAME_MATERIAL, "every frame slot shows the same black pane");
    }

    @DisplayName("A single minigame sits on slot 13, the middle of row one")
    @Test
    void oneMinigameSitsOnSlot13() {
        Assertions.assertEquals(Map.of(Destination.ELYTRA_RACE, 13), NavigatorLayout.mainSlots(List.of(Destination.ELYTRA_RACE)));
    }

    @DisplayName("Two minigames sit on slots 12 and 13")
    @Test
    void twoMinigamesSitOnSlots12And13() {
        Map<Destination, Integer> slots = NavigatorLayout.mainSlots(List.of(Destination.ELYTRA_RACE, Destination.SLENDER));

        Assertions.assertEquals(12, slots.get(Destination.ELYTRA_RACE), "the first minigame is on slot 12");
        Assertions.assertEquals(13, slots.get(Destination.SLENDER), "the second minigame is on slot 13");
    }

    @DisplayName("Three minigames sit on slots 12 to 14")
    @Test
    void threeMinigamesSitOnSlots12To14() {
        Map<Destination, Integer> slots = NavigatorLayout.mainSlots(List.of(Destination.ELYTRA_RACE, Destination.BOUNCE, Destination.SLENDER));

        Assertions.assertEquals(12, slots.get(Destination.ELYTRA_RACE));
        Assertions.assertEquals(13, slots.get(Destination.BOUNCE));
        Assertions.assertEquals(14, slots.get(Destination.SLENDER));
    }

    @DisplayName("Four minigames fill slots 11 to 14 in the order they are declared")
    @Test
    void fourMinigamesFillSlots11To14() {
        Map<Destination, Integer> slots = NavigatorLayout.mainSlots(FOUR_MINIGAMES);

        Assertions.assertEquals(11, slots.get(Destination.ELYTRA_RACE));
        Assertions.assertEquals(12, slots.get(Destination.BOUNCE));
        Assertions.assertEquals(13, slots.get(Destination.TAMIAS));
        Assertions.assertEquals(14, slots.get(Destination.SLENDER));
    }

    @DisplayName("A hidden minigame leaves no gap, so the rest move together")
    @Test
    void hiddenMinigameLeavesNoGap() {
        Map<Destination, Integer> slots = NavigatorLayout.mainSlots(List.of(Destination.ELYTRA_RACE, Destination.TAMIAS, Destination.SLENDER));

        Assertions.assertEquals(List.of(12, 13, 14), List.of(slots.get(Destination.ELYTRA_RACE), slots.get(Destination.TAMIAS), slots.get(Destination.SLENDER)));
    }

    @DisplayName("One server sits on slot 22, the middle of row two")
    @Test
    void oneServerSitsOnSlot22() {
        Assertions.assertEquals(Map.of(Destination.CREATIVE, 22), NavigatorLayout.mainSlots(List.of(Destination.CREATIVE)));
    }

    @DisplayName("Two servers sit on slots 21 and 22")
    @Test
    void twoServersSitOnSlots21And22() {
        Map<Destination, Integer> slots = NavigatorLayout.mainSlots(List.of(Destination.SURVIVAL, Destination.CREATIVE));

        Assertions.assertEquals(21, slots.get(Destination.SURVIVAL));
        Assertions.assertEquals(22, slots.get(Destination.CREATIVE));
    }

    @DisplayName("Three servers sit on slots 21 to 23")
    @Test
    void threeServersSitOnSlots21To23() {
        Map<Destination, Integer> slots = NavigatorLayout.mainSlots(THREE_SERVERS);

        Assertions.assertEquals(21, slots.get(Destination.SURVIVAL));
        Assertions.assertEquals(22, slots.get(Destination.CREATIVE));
        Assertions.assertEquals(23, slots.get(Destination.BUILD));
    }

    @DisplayName("Minigames and servers never share a slot, even with every entry visible")
    @Test
    void groupsDoNotOverlap() {
        Map<Destination, Integer> slots = NavigatorLayout.mainSlots(ALL_MAIN);

        Assertions.assertEquals(ALL_MAIN.size(), new HashSet<>(slots.values()).size(), "every visible entry needs its own slot, got " + slots);
    }

    @DisplayName("No main-grid entry is placed on the frame")
    @Test
    void mainEntriesStayInsideTheFrame() {
        for (int slot : NavigatorLayout.mainSlots(ALL_MAIN).values()) {
            Assertions.assertFalse(NavigatorLayout.isFrame(slot), "slot " + slot + " must not be on the frame");
        }
    }

    @DisplayName("Spawn sits in the bottom centre and replaces the frame pane there")
    @Test
    void spawnIsBottomCentre() {
        Assertions.assertEquals(31, NavigatorLayout.SPAWN_SLOT, "spawn is in the middle of the bottom row");
        Assertions.assertTrue(NavigatorLayout.isFrame(NavigatorLayout.SPAWN_SLOT), "the bottom row is the frame, spawn takes its middle");
    }

    @DisplayName("The staging hub sits in the bottom right corner, on the frame")
    @Test
    void stagingHubSitsInTheBottomRightCorner() {
        Assertions.assertEquals(35, NavigatorLayout.STAGING_HUB_SLOT, "the staging hub is on slot 35");
        Assertions.assertTrue(NavigatorLayout.isFrame(NavigatorLayout.STAGING_HUB_SLOT), "the hub replaces the frame pane in the corner");
    }
}
