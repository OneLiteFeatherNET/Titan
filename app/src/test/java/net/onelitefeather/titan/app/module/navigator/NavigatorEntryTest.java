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
package net.onelitefeather.titan.app.module.navigator;

import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link NavigatorEntry}'s compact constructor: the slot range it accepts and the
 * fields it requires.
 */
class NavigatorEntryTest {

    private static final ItemStack ICON = ItemStack.of(Material.FEATHER);
    private static final Component NAME = Component.text("Survival");

    @DisplayName("A slot within 0-8 is accepted")
    @Test
    void slotWithinRangeIsAccepted() {
        NavigatorEntry entry = new NavigatorEntry(4, ICON, NAME, "Survival");

        Assertions.assertEquals(4, entry.slot());
        Assertions.assertEquals(ICON, entry.icon());
        Assertions.assertEquals(NAME, entry.displayName());
        Assertions.assertEquals("Survival", entry.destination());
    }

    @DisplayName("A negative slot is rejected")
    @Test
    void negativeSlotIsRejected() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> new NavigatorEntry(-1, ICON, NAME, "Survival"));

        Assertions.assertTrue(thrown.getMessage().contains("-1"), "the message must name the offending slot");
    }

    @DisplayName("A slot past 8 (outside CHEST_1_ROW) is rejected")
    @Test
    void slotPastEightIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new NavigatorEntry(9, ICON, NAME, "Survival"));
    }

    @DisplayName("A null icon is rejected")
    @Test
    void nullIconIsRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> new NavigatorEntry(0, null, NAME, "Survival"));
    }

    @DisplayName("A null display name is rejected")
    @Test
    void nullDisplayNameIsRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> new NavigatorEntry(0, ICON, null, "Survival"));
    }

    @DisplayName("A null destination is rejected")
    @Test
    void nullDestinationIsRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> new NavigatorEntry(0, ICON, NAME, null));
    }

    @DisplayName("A blank destination is rejected")
    @Test
    void blankDestinationIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new NavigatorEntry(0, ICON, NAME, "   "));
    }
}
