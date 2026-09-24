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

import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntry;

/**
 * The one Minestom {@link Inventory} every player's navigator opens into.
 *
 * <p>See {@code openspec/changes/lobby-feature-modules/design.md}, decision 8: one shared
 * {@code CHEST_1_ROW} inventory for every player, instead of the old per-player Caffeine cache of
 * Aves {@code PersonalInventoryBuilder}s that never unregistered a replaced builder. {@link
 * #current()} builds it synchronously on first use and again whenever {@link
 * NavigatorEntries#version()} has changed since the last build - never on a schedule, and never
 * once per player or per open.
 */
final class SharedNavigatorInventory {

    private final Component title;
    private final NavigatorEntries entries;
    private Inventory inventory;
    private NavigatorLayout layout = NavigatorLayout.of(List.of());
    private long builtAtVersion = -1;

    /**
     * @param title   the inventory's title
     * @param entries the platform-wide registry this inventory renders - every module's
     *                contributions, not just the navigator module's own
     */
    SharedNavigatorInventory(Component title, NavigatorEntries entries) {
        this.title = title;
        this.entries = entries;
    }

    /**
     * @return the shared inventory, rebuilt first if {@code entries} changed since the last call
     */
    synchronized Inventory current() {
        long version = this.entries.version();
        if (this.inventory == null || this.builtAtVersion != version) {
            this.layout = NavigatorLayout.of(this.entries.entries());
            Inventory freshInventory = new Inventory(InventoryType.CHEST_1_ROW, this.title);
            for (int slot = 0; slot < NavigatorLayout.SLOT_COUNT; slot++) {
                freshInventory.setItemStack(slot, this.layout.itemAt(slot));
            }
            this.inventory = freshInventory;
            this.builtAtVersion = version;
        }
        return this.inventory;
    }

    /**
     * @param candidate the inventory to check
     * @return whether {@code candidate} is the inventory the last {@link #current()} call returned
     */
    synchronized boolean isCurrent(AbstractInventory candidate) {
        return this.inventory != null && this.inventory == candidate;
    }

    /**
     * @param slot the clicked slot
     * @return the entry occupying {@code slot} in the layout {@link #current()} last built, if any
     */
    synchronized Optional<NavigatorEntry> entryAt(int slot) {
        return this.layout.entryAt(slot);
    }
}
