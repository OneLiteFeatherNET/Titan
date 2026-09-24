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
 * Covers the {@code lobby-hotbar} spec requirement that a placement conflict's message names the
 * contested placement and both modules.
 */
class ItemPlacementConflictExceptionTest {

    @DisplayName("The message names a contested hotbar slot and both modules")
    @Test
    void messageNamesAContestedHotbarSlotAndBothModules() {
        SlotConflictDetector.Conflict conflict = new SlotConflictDetector.Conflict(ItemSlot.hotbar(4), "navigator", "friends");

        ItemPlacementConflictException exception = new ItemPlacementConflictException(conflict);

        Assertions.assertTrue(exception.getMessage().contains("navigator"), "must name the first module");
        Assertions.assertTrue(exception.getMessage().contains("friends"), "must name the second module");
        Assertions.assertTrue(exception.getMessage().contains("4"), "must name the contested slot");
    }

    @DisplayName("The message names a contested equipment slot")
    @Test
    void messageNamesAContestedEquipmentSlot() {
        SlotConflictDetector.Conflict conflict = new SlotConflictDetector.Conflict(ItemSlot.equipment(EquipmentSlot.CHESTPLATE), "spawn", "cosmetics");

        ItemPlacementConflictException exception = new ItemPlacementConflictException(conflict);

        Assertions.assertTrue(exception.getMessage().contains("CHESTPLATE"), "must name the contested equipment slot");
    }
}
