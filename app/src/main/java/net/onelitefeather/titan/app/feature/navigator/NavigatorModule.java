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

import io.avaje.inject.Priority;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;
import net.onelitefeather.titan.app.module.item.ItemSlot;
import net.onelitefeather.titan.app.module.item.LobbyItem;
import net.onelitefeather.titan.common.feature.FeatureFlags;
import net.theevilreaper.aves.inventory.GlobalInventoryBuilder;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;

/**
 * The lobby's navigator: a feather in hotbar slot 4 that opens one Aves-built inventory, shared by
 * every player, listing the four destinations fixed in {@link Destination}.
 *
 * <p>See {@code openspec/changes/navigator-entries-in-code/design.md}, decisions 1 and 3, and the
 * {@code lobby-navigator} spec. This module owns one Aves {@link GlobalInventoryBuilder} directly -
 * built once in the constructor, registered exactly once in {@link #enable} and unregistered
 * exactly once in {@link #disable} - instead of the former, separate registry/layout/visibility
 * classes: with only this module ever contributing a destination, that machinery was YAGNI (design
 * .md, decision 1). Every occupied slot's Aves click handler cancels the click, forwards through
 * {@link Deliver} and closes the inventory; every other slot is a gray glass pane. Because Aves
 * maps its click listener directly onto the built inventory, and Minestom dispatches a mapped
 * inventory's handlers before any regular event node's listeners, this click handling runs (and
 * completes) before {@code feature.protection.ProtectionModule} - or any other module - could ever
 * cancel the event first, independent of which module enabled first.
 *
 * <p>{@link #featureFlags} gates {@link Destination#SLENDER} only (see {@code design.md}, decision
 * 3): injected via the constructor rather than read through the static {@code io.avaje.config
 * .Config} facade, so a test can hand in a fake. The {@code titan:navigator} item's use handler
 * calls {@link #applyLayoutIfChanged()} before every open, which re-applies the layout only if
 * {@link Destination#visible(FeatureFlags)} differs from the set last applied - so a flag flip at
 * runtime shows up on the very next open, without a module restart, and without rebuilding the
 * layout on every single open.
 */
@Singleton
@Priority(400)
public final class NavigatorModule implements LobbyModule {

    private static final String ID = "navigator";
    private static final Key ITEM_KEY = Key.key("titan:navigator");
    private static final int HOTBAR_SLOT = 4;
    private static final int SLOT_COUNT = 9;
    private static final ItemStack BLANK = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE).customName(Component.empty()).build();

    private final Deliver deliver;
    private final FeatureFlags featureFlags;
    private final GlobalInventoryBuilder builder;
    private List<Destination> appliedVisible;

    /**
     * @param deliver      the delivery service a navigator click forwards the player through
     * @param featureFlags the source of truth {@link Destination#SLENDER}'s feature gate is
     *                     checked against
     */
    public NavigatorModule(Deliver deliver, FeatureFlags featureFlags) {
        this.deliver = Objects.requireNonNull(deliver, "deliver must not be null");
        this.featureFlags = Objects.requireNonNull(featureFlags, "featureFlags must not be null");
        this.builder = new GlobalInventoryBuilder(MiniMessage.miniMessage().deserialize("<yellow>Navigator"), InventoryType.CHEST_1_ROW);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void enable(ModuleContext context) {
        applyLayoutIfChanged();

        ItemStack feather = ItemStack.builder(Material.FEATHER).customName(MiniMessage.miniMessage().deserialize("<!i><aqua>Navigator")).build();
        context.items().register(new LobbyItem(ID, ITEM_KEY, feather, ItemSlot.hotbar(HOTBAR_SLOT), (player, event) -> {
            applyLayoutIfChanged();
            player.openInventory(this.builder.getInventory());
        }));

        this.builder.register();
    }

    @Override
    public void disable() {
        this.builder.unregister();
    }

    /**
     * Test-only: the shared Aves inventory this module opens for every player, so a leak test can
     * assert the listener count on the event node Aves registered on stays constant across opens.
     *
     * @return the shared inventory
     */
    Inventory sharedInventory() {
        return this.builder.getInventory();
    }

    /**
     * Rebuilds and applies the Aves layout if {@link Destination#visible(FeatureFlags)} differs
     * from the set last applied here - a no-op otherwise. Synchronized so two threads opening the
     * navigator at the same time can never observe, or trigger, half of a rebuild (see {@code
     * design.md}, decision 3).
     */
    private synchronized void applyLayoutIfChanged() {
        List<Destination> visible = Destination.visible(this.featureFlags);
        if (visible.equals(this.appliedVisible)) {
            return;
        }
        this.builder.setLayout(toAvesLayout(visible));
        this.builder.invalidateLayout();
        this.appliedVisible = visible;
    }

    /**
     * @param visible the destinations to place, one per {@link Destination#slot()}
     * @return an Aves {@link InventoryLayout} with a gray glass pane in every slot, then every
     *         visible destination's icon and click handler on top
     */
    private InventoryLayout toAvesLayout(List<Destination> visible) {
        InventoryLayout layout = InventoryLayout.fromType(InventoryType.CHEST_1_ROW);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            layout.setItem(slot, BLANK);
        }
        for (Destination destination : visible) {
            layout.setItem(destination.slot(), destination.item(), (player, clickedSlot, click, stack, result) -> {
                result.accept(ClickHolder.cancelClick());
                this.deliver.sendPlayer(player, DeliverComponent.taskBuilder().taskName(destination.task()).player(player).build());
                player.closeInventory();
            });
        }
        return layout;
    }
}
