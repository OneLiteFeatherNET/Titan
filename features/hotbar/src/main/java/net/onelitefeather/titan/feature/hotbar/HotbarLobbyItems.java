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
package net.onelitefeather.titan.feature.hotbar;

import io.avaje.inject.PreDestroy;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.core.module.item.LobbyItems;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Platform-wide home for {@link LobbyItem} beans: every feature that has one contributes it
 * through Avaje Inject's list injection. {@link #equip(Player)} clears the player's inventory and
 * places every item with a fixed placement, while {@link #stack(String)} hands back the stamped
 * stack for an item with no fixed placement. The item maps are built once by the constructor and
 * never mutated, so no synchronization is needed for the tick-thread reads in {@link #dispatch}.
 *
 * <p>Every other column depends only on the {@link LobbyItems} interface in {@code core}, never on
 * this class.
 */
@Singleton
public final class HotbarLobbyItems implements LobbyItems {

    /** A stack without this tag is not a registered item and {@link #dispatch} leaves it alone. */
    public static final Tag<String> IDENTITY_TAG = Tag.String("titan:item");

    private final Map<String, LobbyItem> itemsByKey;
    private final EventNode<Event> titan;
    private final Telemetry telemetry;
    private final HotbarTelemetry hotbarTelemetry;
    private final EventListener<PlayerUseItemEvent> dispatcher;

    /** @throws IllegalStateException if two items conflict */
    public HotbarLobbyItems(List<LobbyItem> items, @Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Telemetry telemetry) {
        ItemConflicts.check(items);
        this.titan = titan;
        this.telemetry = telemetry;
        this.hotbarTelemetry = new HotbarTelemetry(telemetry);
        this.itemsByKey = stampAll(items);
        this.dispatcher = EventListener.of(PlayerUseItemEvent.class, this::dispatch);
        this.titan.addListener(this.dispatcher);
    }

    @Override
    public void equip(Player player) {
        // Rendered per player: a localized item (e.g. a translated name) differs by locale.
        Placements placements = placementsOf(this.itemsByKey.values(), Objects.requireNonNullElse(player.getLocale(), Locale.ENGLISH));
        int placed = placements.hotbar().size() + placements.equipment().size();
        this.hotbarTelemetry.equip(player.getUuid(), placed, () -> {
            player.getInventory().clear();
            placements.hotbar().forEach((slot, stack) -> player.getInventory().setItemStack(slot, stack));
            placements.equipment().forEach(player::setEquipment);
        });
    }

    public int itemCount() {
        return this.itemsByKey.size();
    }

    @Override
    public ItemStack stack(String key) {
        LobbyItem item = this.itemsByKey.get(key);
        if (item == null) {
            throw new IllegalArgumentException("No lobby item registered for key '" + key + "'");
        }
        return item.itemStack();
    }

    @PreDestroy
    public void stop() {
        this.titan.removeListener(this.dispatcher);
    }

    private static Map<String, LobbyItem> stampAll(List<LobbyItem> items) {
        Map<String, LobbyItem> stamped = new LinkedHashMap<>();
        for (LobbyItem item : items) {
            ItemStack stampedStack = item.itemStack().withTag(IDENTITY_TAG, item.key().asString());
            Key key = item.key();
            stamped.put(key.asString(), new LobbyItem(item.featureId(), key, stampedStack, item.placement(), item.onUse(), locale -> stamp(item.localized().apply(locale), key)));
        }
        return Map.copyOf(stamped);
    }

    private static ItemStack stamp(ItemStack stack, Key key) {
        return stack.withTag(IDENTITY_TAG, key.asString());
    }

    private record Placements(Map<Integer, ItemStack> hotbar,
                              Map<EquipmentSlot, ItemStack> equipment) {
    }

    private static Placements placementsOf(Collection<LobbyItem> items, Locale locale) {
        Map<Integer, ItemStack> hotbarSlots = new LinkedHashMap<>();
        Map<EquipmentSlot, ItemStack> equipmentSlots = new LinkedHashMap<>();
        for (LobbyItem item : items) {
            switch (item.placement()) {
                case ItemSlot.Hotbar slot -> hotbarSlots.put(slot.slot(), item.stackFor(locale));
                case ItemSlot.Equipment slot ->
                    equipmentSlots.put(slot.slot(), item.stackFor(locale));
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
        Consumer<PlayerUseItemEvent> handler = FeatureNode.guard(item.featureId(), this.telemetry, (PlayerUseItemEvent guardedEvent) -> this.hotbarTelemetry.use(item.featureId(), keyValue, guardedEvent.getPlayer().getUuid(), () -> item.onUse().handle(guardedEvent.getPlayer(), guardedEvent)));
        handler.accept(event);
    }
}
