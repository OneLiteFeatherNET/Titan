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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.paging.Pagination;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.theevilreaper.aves.inventory.GlobalInventoryBuilder;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;

/**
 * The server list of one minigame in one language, shared by every player who opens it. Aves fixes
 * an inventory's content and size per builder, so there is one {@link GlobalInventoryBuilder} per
 * {@link PickerView}: the order, the page and the row count. A view is built on first use and then
 * reused; nothing is kept per player, the inventory a player has open is their view.
 */
final class SharedServerPicker {

    /**
     * What a named click in the list asks for. The module reads the services again before it shows
     * the list that is asked for.
     */
    interface Clicks {

        void pick(Player player, ServiceCount service);

        /** The sort item was clicked while {@code current} was the order. */
        void sort(Player player, PickerSort current);

        /**
         * A page arrow was clicked in the order {@code current}; {@code page} is counted from zero.
         */
        void turn(Player player, PickerSort current, int page);
    }

    private static final InventoryType[] TYPES = {InventoryType.CHEST_1_ROW, InventoryType.CHEST_2_ROW, InventoryType.CHEST_3_ROW, InventoryType.CHEST_4_ROW, InventoryType.CHEST_5_ROW, InventoryType.CHEST_6_ROW};
    private static final ItemStack BLANK = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE).customName(Component.empty()).build();

    private final Locale language;
    private final Map<PickerView, GlobalInventoryBuilder> builders = new HashMap<>();

    SharedServerPicker(Locale language) {
        this.language = language;
    }

    /**
     * Lays out page {@code page} of {@code running} in the order {@code sort} and returns the
     * inventory that shows it. A page past the last one shows the last one. Call on the tick
     * thread.
     */
    Inventory show(Destination destination, PickerSort sort, int page, List<ServiceCount> running, Clicks clicks) {
        List<ServiceCount> ordered = ServerPicker.entries(running, sort);
        PickerLayout shape = PickerLayout.of(ordered.size());
        int shown = Pagination.clamp(page, ordered.size());
        PickerView view = new PickerView(sort, shown, shape.rows());
        InventoryLayout layout = layout(destination, view, shape, Pagination.slice(ordered, shown), clicks);
        GlobalInventoryBuilder builder = this.builders.get(view);
        if (builder == null) {
            // Aves needs a layout before it can register, so the first one is set before that.
            builder = new GlobalInventoryBuilder(ServerPicker.title(this.language), TYPES[view.rows() - 1]);
            builder.setLayout(layout);
            builder.register();
            this.builders.put(view, builder);
        } else {
            builder.setLayout(layout);
            builder.invalidateLayout();
        }
        return builder.getInventory();
    }

    /** Whether {@code inventory} is one of this list's views. */
    boolean owns(AbstractInventory inventory) {
        return this.builders.values().stream().anyMatch(builder -> builder.getInventory() == inventory);
    }

    /** Removes every view from Aves; idempotent. */
    void unregister() {
        this.builders.values().forEach(GlobalInventoryBuilder::unregister);
        this.builders.clear();
    }

    private InventoryLayout layout(Destination destination, PickerView view, PickerLayout shape, List<ServiceCount> services, Clicks clicks) {
        InventoryLayout layout = InventoryLayout.fromType(TYPES[view.rows() - 1]);
        for (int slot = 0; slot < layout.getSize(); slot++) {
            layout.setItem(slot, BLANK);
        }
        if (services.isEmpty()) {
            inert(layout, ServerPicker.EMPTY_SLOT, ServerPicker.emptyItem(this.language));
        }
        for (int index = 0; index < services.size(); index++) {
            ServiceCount service = services.get(index);
            button(layout, PickerLayout.serviceSlot(index, services.size()), ServerPicker.item(destination, service, this.language), (player, click) -> clicks.pick(player, service));
        }
        button(layout, shape.sortSlot(), ServerPicker.sortItem(view.sort(), this.language, view.page(), shape.pages()), (player, click) -> clicks.sort(player, view.sort()));
        if (shape.previousShown(view.page())) {
            button(layout, shape.previousSlot(), ServerPicker.previousItem(view.page(), shape.pages(), this.language), (player, click) -> clicks.turn(player, view.sort(), view.page() - 1));
        }
        if (shape.nextShown(view.page())) {
            button(layout, shape.nextSlot(), ServerPicker.nextItem(view.page(), shape.pages(), this.language), (player, click) -> clicks.turn(player, view.sort(), view.page() + 1));
        }
        return layout;
    }

    // Every click on the list is cancelled, so no icon moves; only a named click acts.
    private static void button(InventoryLayout layout, int slot, ItemStack item, BiConsumer<Player, Click> onNamedClick) {
        layout.setItem(slot, item, (player, clickedSlot, click, stack, result) -> {
            result.accept(ClickHolder.cancelClick());
            if (ClickAction.isNamed(click)) {
                onNamedClick.accept(player, click);
            }
        });
    }

    // A slot that shows its item and does nothing when clicked.
    private static void inert(InventoryLayout layout, int slot, ItemStack item) {
        button(layout, slot, item, (player, click) -> {
        });
    }

    /** The inventory a view is shown in: the order, the page and the rows it has. */
    private record PickerView(PickerSort sort, int page, int rows) {
    }
}
