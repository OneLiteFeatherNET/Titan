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

import io.avaje.inject.PreDestroy;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.onelitefeather.titan.app.module.FeatureNode;
import net.onelitefeather.titan.common.observability.TitanObservability;

/**
 * Platform-wide home for {@link LobbyItem} beans (see
 * {@code openspec/changes/dissolve-module-platform/design.md}, decision 2): every feature that has
 * one contributes it through Avaje Inject's list injection instead of registering it with a
 * module context.
 *
 * <p>The constructor does three things, in order: {@link ItemConflicts#check(List)} aborts the
 * whole {@code BeanScope} build if two items claim the same {@link LobbyItem#key()} or the same
 * fixed {@link ItemSlot}; every item's stack is stamped with {@link #IDENTITY_TAG} so a used stack
 * can be traced back to its {@link ItemUseHandler} regardless of material or display name; and a
 * single guarded {@link PlayerUseItemEvent} dispatcher attaches to the shared {@code titan} node.
 *
 * <p>{@link #equip(Player)} clears the player's inventory and places every item with a fixed
 * placement - the standard lobby loadout is platform-wide, not limited to one feature's own items.
 * {@link #stack(Key)} hands back the stamped stack for an item with no fixed placement (the elytra
 * feature's firework, for instance), which a feature gives out and takes back itself.
 *
 * <h2>Threading</h2>
 *
 * <p>No synchronization guards {@link #itemsByKey}, {@link #hotbar} or {@link #equipment}: all three
 * are immutable ({@link Map#copyOf}) and fully built by the constructor before
 * {@link #dispatcher} - the only thing that reads them afterwards, on the tick thread, for every
 * {@link PlayerUseItemEvent} - is ever registered. A reader can therefore never observe a partially
 * built state, and nothing here is ever written again after construction.
 */
@Singleton
public final class LobbyItems {

    /**
     * Stamped onto every item's stack on construction. Its value is that item's own
     * {@link LobbyItem#key()}, as a string - the identity {@link #dispatch} resolves a used stack
     * back to its handler with. A stack without this tag - a plain feather, say - is not a
     * registered item and is left alone.
     */
    public static final Tag<String> IDENTITY_TAG = Tag.String("titan:item");

    private final Map<String, LobbyItem> itemsByKey;
    private final Map<Integer, ItemStack> hotbar;
    private final Map<EquipmentSlot, ItemStack> equipment;
    private final EventNode<Event> titan;
    private final EventListener<PlayerUseItemEvent> dispatcher;

    /**
     * @param items every {@link LobbyItem} a feature bean contributed, in the order Avaje Inject's
     *              list injection handed them
     * @param titan the shared event node every feature's own node also attaches under; this
     *              instance's dispatcher attaches to it directly
     * @throws IllegalStateException if two items conflict; see {@link ItemConflicts#check(List)}
     */
    public LobbyItems(List<LobbyItem> items, @Named(FeatureNode.TITAN_NODE) EventNode<Event> titan) {
        ItemConflicts.check(items);
        this.titan = titan;
        this.itemsByKey = stampAll(items);
        Placements placements = placementsOf(this.itemsByKey.values());
        this.hotbar = placements.hotbar();
        this.equipment = placements.equipment();
        this.dispatcher = EventListener.of(PlayerUseItemEvent.class, this::dispatch);
        this.titan.addListener(this.dispatcher);
    }

    /**
     * Clears {@code player}'s inventory and sets every item that has a fixed placement. The
     * player's inventory contains exactly the currently known items afterwards, and nothing else.
     *
     * @param player the player to equip
     */
    public void equip(Player player) {
        player.getInventory().clear();
        this.hotbar.forEach((slot, stack) -> player.getInventory().setItemStack(slot, stack));
        this.equipment.forEach(player::setEquipment);
    }

    /**
     * @param key an item's key
     * @return the stamped stack for that item - for handing out an item with no fixed placement,
     *         such as the elytra feature's firework
     * @throws IllegalArgumentException if no item with that key exists
     */
    public ItemStack stack(Key key) {
        LobbyItem item = this.itemsByKey.get(key.asString());
        if (item == null) {
            throw new IllegalArgumentException("No lobby item registered for key '" + key.asString() + "'");
        }
        return item.itemStack();
    }

    /** Detaches this instance's dispatcher from the {@code titan} node. */
    @PreDestroy
    public void stop() {
        this.titan.removeListener(this.dispatcher);
    }

    private static Map<String, LobbyItem> stampAll(List<LobbyItem> items) {
        Map<String, LobbyItem> stamped = new LinkedHashMap<>();
        for (LobbyItem item : items) {
            ItemStack stampedStack = item.itemStack().withTag(IDENTITY_TAG, item.key().asString());
            stamped.put(item.key().asString(), new LobbyItem(item.featureId(), item.key(), stampedStack, item.placement(), item.onUse()));
        }
        return Map.copyOf(stamped);
    }

    /**
     * The hotbar and equipment slots {@link #equip(Player)} fills, computed once in a single pass
     * over every item - mirroring {@link EquipPlan#from(Collection)}.
     */
    private record Placements(Map<Integer, ItemStack> hotbar, Map<EquipmentSlot, ItemStack> equipment) {
    }

    private static Placements placementsOf(Collection<LobbyItem> items) {
        Map<Integer, ItemStack> hotbarSlots = new LinkedHashMap<>();
        Map<EquipmentSlot, ItemStack> equipmentSlots = new LinkedHashMap<>();
        for (LobbyItem item : items) {
            switch (item.placement()) {
                case ItemSlot.Hotbar slot -> hotbarSlots.put(slot.slot(), item.itemStack());
                case ItemSlot.Equipment slot -> equipmentSlots.put(slot.slot(), item.itemStack());
                case ItemSlot.Unplaced ignored -> {
                    // Given out and taken back by the owning feature itself; equip() never places
                    // it.
                }
            }
        }
        return new Placements(Map.copyOf(hotbarSlots), Map.copyOf(equipmentSlots));
    }

    private void dispatch(PlayerUseItemEvent event) {
        String keyValue = event.getItemStack().getTag(IDENTITY_TAG);
        if (keyValue == null) {
            return;
        }
        LobbyItem item = this.itemsByKey.get(keyValue);
        if (item == null) {
            return;
        }
        Consumer<PlayerUseItemEvent> handler = TitanObservability.guard(item.featureId(), (PlayerUseItemEvent guardedEvent) -> item.onUse().handle(guardedEvent.getPlayer(), guardedEvent));
        handler.accept(event);
    }
}
