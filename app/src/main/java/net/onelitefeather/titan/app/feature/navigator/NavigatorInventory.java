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
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntry;
import net.onelitefeather.titan.common.feature.FeatureFlags;
import net.theevilreaper.aves.inventory.GlobalInventoryBuilder;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;

/**
 * The one Aves-built {@link Inventory} every player's navigator opens into.
 *
 * <p>See {@code openspec/changes/lobby-feature-modules/design.md}, decision 8: one shared
 * {@code CHEST_1_ROW} inventory for every player, built and owned by an Aves
 * {@link GlobalInventoryBuilder} instead of a raw Minestom {@code Inventory} with a hand-rolled
 * click listener - the project runs every inventory through Aves. {@link #register()} and
 * {@link #unregister()} are each meant to be called exactly once, from {@code NavigatorModule}'s
 * {@code enable} and {@code disable}, never per player or per open - so Aves registers its click
 * listener on the built inventory's own event node exactly once for this module's whole lifetime.
 *
 * <p>{@link #current()} returns the up-to-date {@link Inventory}, rebuilding the Aves
 * {@link InventoryLayout} first if the set of <em>visible</em> entries - every registry entry with
 * no {@link NavigatorEntry#feature()}, plus every one whose feature is currently active according
 * to
 * a {@link FeatureFlags} source (see {@link NavigatorVisibility}) - differs from the set last
 * applied. That covers both a change to {@link NavigatorEntries#version()} (an entry was added or
 * removed) and a feature flag being toggled at runtime, without this class needing to track the two
 * separately: see {@code openspec/changes/lobby-feature-modules/design.md}, decision 13. Never
 * rebuilt on a schedule, and never once per player or per open. Rebuilding only replaces the
 * builder's static layout ({@link GlobalInventoryBuilder#getInventory} then applies it
 * synchronously) - this never needs Aves' data-layout, next-tick scheduling path, so opening the
 * navigator never needs a server tick to show the right content.
 *
 * <p>Every occupied slot's Aves click handler cancels the click, hands the click off to
 * {@code onSelect} (which the owning module wires to forward through {@code Deliver}) and closes
 * the inventory; every other slot keeps Aves' default cancel-only click handler. Because Aves maps
 * its click listener directly on the built inventory itself - and Minestom's event dispatcher
 * always
 * runs a mapped inventory's listeners before it descends into any regular event node, including a
 * module's own - this click handling runs, and completes, before
 * {@code feature.protection.ProtectionModule}'s node (or any other module's) can ever cancel the
 * event first. That makes this module's click handling independent of whether
 * {@code ProtectionModule} is enabled before or after it, without this module needing an
 * {@code InventoryPreClickEvent} listener of its own.
 */
final class NavigatorInventory {

    private final NavigatorEntries entries;
    private final FeatureFlags featureFlags;
    private final BiConsumer<Player, NavigatorEntry> onSelect;
    private final GlobalInventoryBuilder builder;
    private List<NavigatorEntry> appliedVisibleEntries;

    /**
     * @param title        the inventory's title
     * @param entries      the platform-wide registry this inventory renders - every module's
     *                     contributions, not just the owning module's own
     * @param featureFlags the source of truth for whether an entry's optional feature gate is
     *                     currently active
     * @param onSelect     called with the clicking player and the entry occupying the clicked slot,
     *                     after the click is cancelled and before the inventory is closed
     */
    NavigatorInventory(Component title, NavigatorEntries entries, FeatureFlags featureFlags, BiConsumer<Player, NavigatorEntry> onSelect) {
        this.entries = entries;
        this.featureFlags = featureFlags;
        this.onSelect = onSelect;
        this.builder = new GlobalInventoryBuilder(title, InventoryType.CHEST_1_ROW);
    }

    /**
     * Applies the current layout, then registers Aves' listeners on the inventory's own event node.
     * Call exactly once, from {@code enable()}.
     */
    void register() {
        applyLayoutIfChanged();
        this.builder.register();
    }

    /**
     * Removes Aves' listeners from the inventory's own event node, closing the inventory for anyone
     * who still has it open. Call exactly once, from {@code disable()}.
     */
    void unregister() {
        this.builder.unregister();
    }

    /**
     * @return the shared inventory, rebuilt first if {@code entries} changed since the last call to
     *         this method or to {@link #register()}
     */
    synchronized Inventory current() {
        applyLayoutIfChanged();
        return this.builder.getInventory();
    }

    /**
     * Rebuilds and applies the Aves layout if the current set of visible entries (see
     * {@link NavigatorVisibility}) differs from the set last applied here - a no-op otherwise.
     * Synchronized so two threads opening the navigator at the same time can never observe, or
     * trigger, half of a rebuild.
     */
    private synchronized void applyLayoutIfChanged() {
        // Recomputed on every open by design, not cached across opens: a flag flip has no push
        // signal to this class, so re-checking here is the only way to notice one. The cost is
        // small - one Togglz lookup per flagged entry - and Togglz itself re-reads flags.properties
        // at most once per second, not on every isActive() call.
        List<NavigatorEntry> visible = NavigatorVisibility.visible(this.entries.entries(), this.featureFlags);
        if (visible.equals(this.appliedVisibleEntries)) {
            return;
        }
        this.builder.setLayout(toAvesLayout(NavigatorLayout.of(visible)));
        this.builder.invalidateLayout();
        this.appliedVisibleEntries = visible;
    }

    /**
     * @param layout the pure slot -&gt; item/entry computation to translate
     * @return an Aves {@link InventoryLayout} with the same content: an occupied slot gets the
     *         entry's icon and a click handler that cancels, forwards to {@link #onSelect} and
     *         closes the inventory; every other slot gets {@link NavigatorLayout#BLANK} and Aves'
     *         default cancel-only click handler
     */
    private InventoryLayout toAvesLayout(NavigatorLayout layout) {
        InventoryLayout avesLayout = InventoryLayout.fromType(InventoryType.CHEST_1_ROW);
        for (int slot = 0; slot < NavigatorLayout.SLOT_COUNT; slot++) {
            Optional<NavigatorEntry> entry = layout.entryAt(slot);
            if (entry.isPresent()) {
                NavigatorEntry navigatorEntry = entry.get();
                avesLayout.setItem(slot, navigatorEntry.icon(), (player, clickedSlot, click, stack, result) -> {
                    result.accept(ClickHolder.cancelClick());
                    this.onSelect.accept(player, navigatorEntry);
                    player.closeInventory();
                });
            } else {
                avesLayout.setItem(slot, layout.itemAt(slot));
            }
        }
        return avesLayout;
    }
}
