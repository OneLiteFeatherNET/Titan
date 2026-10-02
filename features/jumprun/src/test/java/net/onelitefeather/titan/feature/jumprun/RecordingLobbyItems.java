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
package net.onelitefeather.titan.feature.jumprun;

import java.util.ArrayList;
import java.util.List;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.module.item.LobbyItems;

/**
 * A stand-in for the hotbar's loadout: an elytra and one item in slot 0, and a note of who got it.
 */
final class RecordingLobbyItems implements LobbyItems {

    static final ItemStack ELYTRA = ItemStack.of(Material.ELYTRA);
    static final ItemStack SLOT_ZERO = ItemStack.of(Material.COMPASS);

    private final List<Player> equipped = new ArrayList<>();

    @Override
    public void equip(Player player) {
        equipped.add(player);
        player.getInventory().clear();
        player.setEquipment(EquipmentSlot.CHESTPLATE, ELYTRA);
        player.getInventory().setItemStack(0, SLOT_ZERO);
    }

    @Override
    public ItemStack stack(String key) {
        throw new UnsupportedOperationException("the module never asks for a stack: " + key);
    }

    List<Player> equipped() {
        return List.copyOf(equipped);
    }
}
