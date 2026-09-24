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

import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;
import net.onelitefeather.titan.app.module.item.ItemSlot;
import net.onelitefeather.titan.app.module.item.LobbyItem;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntry;

/**
 * The lobby's navigator: a feather in hotbar slot 4 that opens one Minestom inventory shared by
 * every player, listing every destination contributed to {@link NavigatorEntries} - this module's
 * own configured destinations and any other module's.
 *
 * <p>See {@code openspec/changes/lobby-feature-modules/design.md}, decision 8, and the
 * {@code lobby-navigator} spec. This replaces the old {@code NavigationHelper}: one shared
 * inventory ({@link SharedNavigatorInventory}), rebuilt only when
 * {@link NavigatorEntries#version()}
 * changes, and exactly one {@link InventoryPreClickEvent} listener, registered once in
 * {@link #enable}, instead of a per-player Caffeine cache of Aves inventory builders whose click
 * listeners were never unregistered on refresh.
 *
 * <p>{@link #entries} is handed in through the constructor rather than read from {@code context},
 * because {@link ModuleContext#navigator()} only exposes the narrow, add-only
 * {@link NavigatorEntries.View} - this module needs to read back every module's entries at open
 * time, not just add its own.
 */
public final class NavigatorModule implements LobbyModule {

    private static final String ID = "navigator";
    private static final Key ITEM_KEY = Key.key("titan:navigator");
    private static final int HOTBAR_SLOT = 4;

    private final Deliver deliver;
    private final NavigatorEntries entries;
    private SharedNavigatorInventory sharedInventory;

    /**
     * @param deliver the delivery service a navigator click forwards the player through
     * @param entries the platform-wide navigator entry registry this module renders
     */
    public NavigatorModule(Deliver deliver, NavigatorEntries entries) {
        this.deliver = Objects.requireNonNull(deliver, "deliver must not be null");
        this.entries = Objects.requireNonNull(entries, "entries must not be null");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void enable(ModuleContext context) {
        NavigatorConfig config = context.config(NavigatorConfig.class, NavigatorConfig.DEFAULTS);
        Component title = MiniMessage.miniMessage().deserialize(config.title());
        this.sharedInventory = new SharedNavigatorInventory(title, this.entries);

        for (NavigatorConfig.Entry entry : config.entries()) {
            context.navigator().add(toNavigatorEntry(entry));
        }

        ItemStack feather = ItemStack.builder(Material.FEATHER).customName(MiniMessage.miniMessage().deserialize("<!i><aqua>Navigator")).build();
        context.items().register(new LobbyItem(ITEM_KEY, feather, ItemSlot.hotbar(HOTBAR_SLOT), (player, event) -> player.openInventory(this.sharedInventory.current())));

        context.listen(InventoryPreClickEvent.class, this::onClick);
    }

    private void onClick(InventoryPreClickEvent event) {
        if (!this.sharedInventory.isCurrent(event.getInventory())) {
            return;
        }
        event.setCancelled(true);
        this.sharedInventory.entryAt(event.getSlot()).ifPresent(entry -> this.deliver.sendPlayer(event.getPlayer(), DeliverComponent.taskBuilder().taskName(entry.destination()).player(event.getPlayer()).build()));
        event.getPlayer().closeInventory();
    }

    private static NavigatorEntry toNavigatorEntry(NavigatorConfig.Entry entry) {
        Component displayName = MiniMessage.miniMessage().deserialize(entry.displayName());
        ItemStack icon = ItemStack.builder(Material.fromKey(entry.icon())).customName(displayName).build();
        return new NavigatorEntry(entry.slot(), icon, displayName, entry.destination());
    }
}
