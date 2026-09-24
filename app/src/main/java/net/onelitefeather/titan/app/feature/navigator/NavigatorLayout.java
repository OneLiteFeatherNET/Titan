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
package net.onelitefeather.titan.app.feature.navigator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntry;

/**
 * Which item belongs in which slot of the shared navigator inventory: every {@link NavigatorEntry}
 * under its own slot, and a gray glass pane everywhere else.
 *
 * <p>Kept apart from {@link NavigatorInventory} - which owns the actual Minestom
 * {@code Inventory} - so this placement logic is testable as plain data, the same way
 * {@code net.onelitefeather.titan.app.module.item.EquipPlan} is kept apart from applying itself to
 * a player.
 */
final class NavigatorLayout {

    /** {@code CHEST_1_ROW} always has exactly nine slots, {@code 0}-{@code 8}. */
    static final int SLOT_COUNT = 9;

    /** Fills every slot without an entry. */
    static final ItemStack BLANK = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE).customName(Component.empty()).build();

    private final Map<Integer, NavigatorEntry> bySlot;

    private NavigatorLayout(Map<Integer, NavigatorEntry> bySlot) {
        this.bySlot = bySlot;
    }

    /**
     * @param entries the entries to place, e.g. {@code NavigatorEntries#entries()}'s current
     *                snapshot
     * @return a layout with every entry under its own slot
     */
    static NavigatorLayout of(List<NavigatorEntry> entries) {
        Map<Integer, NavigatorEntry> bySlot = new HashMap<>();
        for (NavigatorEntry entry : entries) {
            bySlot.put(entry.slot(), entry);
        }
        return new NavigatorLayout(bySlot);
    }

    /**
     * @param slot the slot to look up
     * @return the entry's own icon if {@code slot} is occupied, {@link #BLANK} otherwise
     */
    ItemStack itemAt(int slot) {
        NavigatorEntry entry = this.bySlot.get(slot);
        return entry != null ? entry.icon() : BLANK;
    }

    /**
     * @param slot the slot to look up
     * @return the entry occupying {@code slot}, or empty if it is blank
     */
    Optional<NavigatorEntry> entryAt(int slot) {
        return Optional.ofNullable(this.bySlot.get(slot));
    }
}
