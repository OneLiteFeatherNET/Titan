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

import java.util.List;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Covers {@link EquipPlan#from(java.util.Collection)}: which stack goes into which hotbar or
 * equipment slot, kept apart from {@link EquipPlan#applyTo} so the layout is testable without a
 * {@link net.minestom.server.entity.Player}.
 */
@ExtendWith(MicrotusExtension.class)
class EquipPlanTest {

    private static LobbyItem item(String key, ItemStack stack, ItemSlot placement) {
        return new LobbyItem(Key.key(key), stack, placement, (player, event) -> {
        });
    }

    @DisplayName("A hotbar item lands in the plan under its own slot")
    @Test
    void aHotbarItemLandsUnderItsOwnSlot() {
        ItemStack feather = ItemStack.of(Material.FEATHER);

        EquipPlan plan = EquipPlan.from(List.of(item("titan:navigator", feather, ItemSlot.hotbar(4))));

        Assertions.assertEquals(feather, plan.hotbar().get(4));
        Assertions.assertTrue(plan.equipment().isEmpty());
    }

    @DisplayName("An equipment item lands in the plan under its own slot")
    @Test
    void anEquipmentItemLandsUnderItsOwnSlot() {
        ItemStack elytra = ItemStack.of(Material.ELYTRA);

        EquipPlan plan = EquipPlan.from(List.of(item("titan:elytra", elytra, ItemSlot.equipment(EquipmentSlot.CHESTPLATE))));

        Assertions.assertEquals(elytra, plan.equipment().get(EquipmentSlot.CHESTPLATE));
        Assertions.assertTrue(plan.hotbar().isEmpty());
    }

    @DisplayName("An unplaced item is left out of the plan entirely")
    @Test
    void anUnplacedItemIsLeftOut() {
        ItemStack firework = ItemStack.of(Material.FIREWORK_ROCKET);

        EquipPlan plan = EquipPlan.from(List.of(item("titan:firework", firework, ItemSlot.unplaced())));

        Assertions.assertTrue(plan.hotbar().isEmpty());
        Assertions.assertTrue(plan.equipment().isEmpty());
    }

    @DisplayName("Several placed items each land under their own slot")
    @Test
    void severalPlacedItemsEachLandUnderTheirOwnSlot() {
        ItemStack feather = ItemStack.of(Material.FEATHER);
        ItemStack elytra = ItemStack.of(Material.ELYTRA);

        EquipPlan plan = EquipPlan.from(List.of(item("titan:navigator", feather, ItemSlot.hotbar(4)), item("titan:elytra", elytra, ItemSlot.equipment(EquipmentSlot.CHESTPLATE))));

        Assertions.assertEquals(feather, plan.hotbar().get(4));
        Assertions.assertEquals(elytra, plan.equipment().get(EquipmentSlot.CHESTPLATE));
    }
}
