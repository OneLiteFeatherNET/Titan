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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;

/**
 * Which stack goes where, computed once from every currently registered {@link LobbyItem} and kept
 * apart from {@link #applyTo(Player)} so the placement logic itself is testable without a running
 * server or a real {@link Player}.
 */
final class EquipPlan {

    private final Map<Integer, ItemStack> hotbar;
    private final Map<EquipmentSlot, ItemStack> equipment;

    private EquipPlan(Map<Integer, ItemStack> hotbar, Map<EquipmentSlot, ItemStack> equipment) {
        this.hotbar = hotbar;
        this.equipment = equipment;
    }

    /**
     * @param items every currently registered item; items with no fixed placement are ignored
     * @return the plan built from their placements
     */
    static EquipPlan from(Collection<LobbyItem> items) {
        Map<Integer, ItemStack> hotbar = new LinkedHashMap<>();
        Map<EquipmentSlot, ItemStack> equipment = new LinkedHashMap<>();
        for (LobbyItem item : items) {
            switch (item.placement()) {
                case ItemSlot.Hotbar slot -> hotbar.put(slot.slot(), item.itemStack());
                case ItemSlot.Equipment slot -> equipment.put(slot.slot(), item.itemStack());
                case ItemSlot.Unplaced ignored -> {
                    // Gives itself out and takes itself back; equip() never places it.
                }
            }
        }
        return new EquipPlan(hotbar, equipment);
    }

    /**
     * @return the hotbar slots this plan fills, by slot index
     */
    Map<Integer, ItemStack> hotbar() {
        return Collections.unmodifiableMap(this.hotbar);
    }

    /**
     * @return the equipment slots this plan fills
     */
    Map<EquipmentSlot, ItemStack> equipment() {
        return Collections.unmodifiableMap(this.equipment);
    }

    /**
     * Clears {@code player}'s inventory and sets every placed item from this plan, hotbar and
     * equipment alike. A player therefore ends up with exactly the currently registered items and
     * nothing else.
     *
     * @param player the player to equip
     */
    void applyTo(Player player) {
        player.getInventory().clear();
        this.hotbar.forEach((slot, stack) -> player.getInventory().setItemStack(slot, stack));
        this.equipment.forEach(player::setEquipment);
    }
}
