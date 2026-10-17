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

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.theevilreaper.aves.inventory.GlobalInventoryBuilder;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;

/**
 * The server list of one minigame in one language, shared by every player who opens it. Aves fixes
 * an inventory's size when it is built, so there is one {@link GlobalInventoryBuilder} per row
 * count, each created on first use with the title of the language and registered with Aves at that
 * point.
 */
final class SharedServerPicker {

    /** Called with the entry a player clicked. */
    @FunctionalInterface
    interface PickHandler {
        void pick(Player player, ServiceCount service);
    }

    private static final InventoryType[] TYPES = {InventoryType.CHEST_1_ROW, InventoryType.CHEST_2_ROW, InventoryType.CHEST_3_ROW, InventoryType.CHEST_4_ROW, InventoryType.CHEST_5_ROW, InventoryType.CHEST_6_ROW};
    private static final ItemStack BLANK = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE).customName(Component.empty()).build();

    private final Locale language;
    private final GlobalInventoryBuilder[] builders = new GlobalInventoryBuilder[ServerPicker.MAX_ROWS];

    SharedServerPicker(Locale language) {
        this.language = language;
    }

    /**
     * Lays out {@code entries} of {@code destination} in the inventory for their row count and
     * returns it; {@code onPick} gets the entry a named click lands on. With no entries the middle
     * slot says that nothing runs. Call on the tick thread.
     */
    Inventory show(Destination destination, List<ServiceCount> entries, PickHandler onPick) {
        int index = ServerPicker.rows(entries.size()) - 1;
        InventoryLayout layout = InventoryLayout.fromType(TYPES[index]);
        for (int slot = 0; slot < layout.getSize(); slot++) {
            layout.setItem(slot, BLANK);
        }
        if (entries.isEmpty()) {
            layout.setItem(ServerPicker.EMPTY_SLOT, ServerPicker.emptyItem(this.language), (player, clickedSlot, click, stack, result) -> result.accept(ClickHolder.cancelClick()));
        }
        for (int slot = 0; slot < entries.size(); slot++) {
            ServiceCount service = entries.get(slot);
            layout.setItem(slot, ServerPicker.item(destination, service, this.language), (player, clickedSlot, click, stack, result) -> {
                result.accept(ClickHolder.cancelClick());
                if (ClickAction.isNamed(click)) {
                    onPick.pick(player, service);
                }
            });
        }
        GlobalInventoryBuilder builder = this.builders[index];
        if (builder == null) {
            // Aves needs a layout before it can register, so the first one is set before that.
            builder = new GlobalInventoryBuilder(ServerPicker.title(this.language), TYPES[index]);
            builder.setLayout(layout);
            builder.register();
            this.builders[index] = builder;
        } else {
            builder.setLayout(layout);
            builder.invalidateLayout();
        }
        return builder.getInventory();
    }

    /** Removes every builder from Aves; idempotent. */
    void unregister() {
        for (int index = 0; index < this.builders.length; index++) {
            if (this.builders[index] != null) {
                this.builders[index].unregister();
                this.builders[index] = null;
            }
        }
    }
}
