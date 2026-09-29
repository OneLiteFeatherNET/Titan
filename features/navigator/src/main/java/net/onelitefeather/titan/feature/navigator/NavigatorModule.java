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

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.theevilreaper.aves.inventory.GlobalInventoryBuilder;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;

/**
 * The lobby's navigator: a feather in hotbar slot 4 that opens one Aves-built inventory, shared by
 * every player, listing the four destinations fixed in {@link Destination}.
 *
 * <p>Aves maps its click listener directly onto the built {@link GlobalInventoryBuilder} inventory
 * and dispatches it before any regular event node's listeners, so this click handling always
 * completes before another feature could cancel the event first.
 */
@Singleton
public final class NavigatorModule {

    /** This feature's position among its sibling {@link FeatureNode}s. */
    static final int EVENT_PRIORITY = 400;

    private static final String ID = "navigator";
    private static final int SLOT_COUNT = 9;
    private static final ItemStack BLANK = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE).customName(Component.empty()).build();

    private final EventNode<Event> titan;
    private final Deliver deliver;
    private final FeatureFlags featureFlags;
    private final GlobalInventoryBuilder builder;
    private FeatureNode node;
    private List<Destination> appliedVisible;

    public NavigatorModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Deliver deliver, FeatureFlags featureFlags) {
        this.titan = Objects.requireNonNull(titan, "titan must not be null");
        this.deliver = Objects.requireNonNull(deliver, "deliver must not be null");
        this.featureFlags = Objects.requireNonNull(featureFlags, "featureFlags must not be null");
        this.builder = new GlobalInventoryBuilder(MiniMessage.miniMessage().deserialize("<yellow>Navigator"), InventoryType.CHEST_1_ROW);
    }

    @PostConstruct
    void start() {
        // Listener-less: only attached so this feature shows up in the fixed EVENT_PRIORITY order
        // and the leak test; Aves handles every inventory click itself.
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY);
        applyLayoutIfChanged();
        this.builder.register();
    }

    @PreDestroy
    void stop() {
        this.node.close();
        this.builder.unregister();
    }

    void open(Player player) {
        applyLayoutIfChanged();
        player.openInventory(this.builder.getInventory());
    }

    // Test-only: lets a leak test assert the listener count on Aves' event node stays constant
    // across opens.
    Inventory sharedInventory() {
        return this.builder.getInventory();
    }

    // Synchronized so two threads opening the navigator at once can't observe, or trigger, half of
    // a rebuild.
    private synchronized void applyLayoutIfChanged() {
        List<Destination> visible = Destination.visible(this.featureFlags, false);
        if (visible.equals(this.appliedVisible)) {
            return;
        }
        this.builder.setLayout(toAvesLayout(visible));
        this.builder.invalidateLayout();
        this.appliedVisible = visible;
    }

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
