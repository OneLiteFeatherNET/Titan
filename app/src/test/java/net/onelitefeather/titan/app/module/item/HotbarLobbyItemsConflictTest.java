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
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Plain-Java coverage for {@link ItemConflicts#check(List)}: no {@link HotbarLobbyItems}, no server
 * - only {@link LobbyItem}s in, an exception (or nothing) out.
 */
@ExtendWith(MicrotusExtension.class)
class HotbarLobbyItemsConflictTest {

    private static LobbyItem item(String key, Material material, ItemSlot placement) {
        return new LobbyItem("test-feature", Key.key(key), ItemStack.of(material), placement, (player, event) -> {
        });
    }

    @DisplayName("Items with distinct keys and distinct slots never conflict")
    @Test
    void distinctKeysAndSlotsNeverConflict() {
        List<LobbyItem> items = List.of(item("titan:navigator", Material.FEATHER, ItemSlot.hotbar(4)), item("titan:elytra", Material.ELYTRA, ItemSlot.equipment(EquipmentSlot.CHESTPLATE)));

        Assertions.assertDoesNotThrow(() -> ItemConflicts.check(items));
    }

    @DisplayName("Two items claiming the same hotbar slot abort, naming the slot and both items")
    @Test
    void twoItemsClaimingTheSameHotbarSlotAbort() {
        List<LobbyItem> items = List.of(item("titan:navigator", Material.FEATHER, ItemSlot.hotbar(4)), item("titan:friends", Material.COMPASS, ItemSlot.hotbar(4)));

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> ItemConflicts.check(items));

        Assertions.assertTrue(thrown.getMessage().contains("hotbar slot 4"), "message must name the contested slot: " + thrown.getMessage());
        Assertions.assertTrue(thrown.getMessage().contains("titan:navigator"), "message must name the first item: " + thrown.getMessage());
        Assertions.assertTrue(thrown.getMessage().contains("titan:friends"), "message must name the second item: " + thrown.getMessage());
    }

    @DisplayName("Two items claiming the same equipment slot abort, naming the slot and both items")
    @Test
    void twoItemsClaimingTheSameEquipmentSlotAbort() {
        List<LobbyItem> items = List.of(item("titan:elytra", Material.ELYTRA, ItemSlot.equipment(EquipmentSlot.CHESTPLATE)), item("titan:cosmetic-chestplate", Material.LEATHER_CHESTPLATE, ItemSlot.equipment(EquipmentSlot.CHESTPLATE)));

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> ItemConflicts.check(items));

        Assertions.assertTrue(thrown.getMessage().contains("equipment slot CHESTPLATE"), "message must name the contested slot: " + thrown.getMessage());
        Assertions.assertTrue(thrown.getMessage().contains("titan:elytra"));
        Assertions.assertTrue(thrown.getMessage().contains("titan:cosmetic-chestplate"));
    }

    @DisplayName("Two items sharing the same key abort, naming the key and both items")
    @Test
    void twoItemsSharingTheSameKeyAbort() {
        List<LobbyItem> items = List.of(item("titan:navigator", Material.FEATHER, ItemSlot.hotbar(4)), item("titan:navigator", Material.COMPASS, ItemSlot.hotbar(5)));

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> ItemConflicts.check(items));

        Assertions.assertTrue(thrown.getMessage().contains("titan:navigator"), "message must name the contested key: " + thrown.getMessage());
        Assertions.assertTrue(thrown.getMessage().contains("minecraft:feather"), "message must distinguish the first item by its material: " + thrown.getMessage());
        Assertions.assertTrue(thrown.getMessage().contains("minecraft:compass"), "message must distinguish the second item by its material: " + thrown.getMessage());
    }

    @DisplayName("Two unplaced items never conflict, even with each other")
    @Test
    void twoUnplacedItemsNeverConflict() {
        List<LobbyItem> items = List.of(item("titan:firework", Material.FIREWORK_ROCKET, ItemSlot.unplaced()), item("titan:other-firework", Material.FIREWORK_ROCKET, ItemSlot.unplaced()));

        Assertions.assertDoesNotThrow(() -> ItemConflicts.check(items));
    }
}
