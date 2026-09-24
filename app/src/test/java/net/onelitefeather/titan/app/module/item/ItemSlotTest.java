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
package net.onelitefeather.titan.app.module.item;

import net.minestom.server.entity.EquipmentSlot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain-Java coverage for {@link ItemSlot}: hotbar range validation and equality between the three
 * placement shapes. No server needed - {@link ItemSlot} is pure data.
 */
class ItemSlotTest {

    @DisplayName("Hotbar slots 0 and 8 - the ends of the allowed range - are accepted")
    @Test
    void hotbarAcceptsBothEndsOfTheRange() {
        Assertions.assertDoesNotThrow(() -> ItemSlot.hotbar(0));
        Assertions.assertDoesNotThrow(() -> ItemSlot.hotbar(8));
    }

    @DisplayName("A negative hotbar slot is rejected")
    @Test
    void hotbarRejectsANegativeSlot() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> ItemSlot.hotbar(-1));
        Assertions.assertTrue(thrown.getMessage().contains("-1"), "the message must name the offending slot");
    }

    @DisplayName("A hotbar slot past 8 is rejected")
    @Test
    void hotbarRejectsASlotPastTheEnd() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> ItemSlot.hotbar(9));
        Assertions.assertTrue(thrown.getMessage().contains("9"), "the message must name the offending slot");
    }

    @DisplayName("An equipment placement requires a slot")
    @Test
    void equipmentRejectsANullSlot() {
        Assertions.assertThrows(NullPointerException.class, () -> ItemSlot.equipment(null));
    }

    @DisplayName("Two hotbar placements for the same slot are equal")
    @Test
    void twoHotbarPlacementsForTheSameSlotAreEqual() {
        Assertions.assertEquals(ItemSlot.hotbar(4), ItemSlot.hotbar(4));
    }

    @DisplayName("A hotbar placement and an equipment placement are never equal")
    @Test
    void aHotbarPlacementAndAnEquipmentPlacementAreNeverEqual() {
        Assertions.assertNotEquals(ItemSlot.hotbar(4), ItemSlot.equipment(EquipmentSlot.CHESTPLATE));
    }

    @DisplayName("Two unplaced placements are equal")
    @Test
    void twoUnplacedPlacementsAreEqual() {
        Assertions.assertEquals(ItemSlot.unplaced(), ItemSlot.unplaced());
    }
}
