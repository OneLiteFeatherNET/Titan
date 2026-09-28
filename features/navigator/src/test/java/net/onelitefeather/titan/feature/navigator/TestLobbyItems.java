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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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

/**
 * A test-only {@link LobbyItems}: a column depends only on that interface, never on
 * {@code HotbarLobbyItems} (which lives in {@code features/hotbar}), so this fixture reproduces
 * just enough of its identity-tag dispatch (stamp on construction, look up and guard-dispatch on
 * {@link PlayerUseItemEvent}) for {@link NavigatorModule}'s feather to react to a use exactly as
 * production does.
 */
final class TestLobbyItems implements LobbyItems {

    private static final Tag<String> IDENTITY_TAG = Tag.String("titan:item");

    private final Map<String, LobbyItem> itemsByKey;
    private final Map<Integer, ItemStack> hotbar;
    private final EventNode<Event> titan;
    private final EventListener<PlayerUseItemEvent> dispatcher;

    TestLobbyItems(List<LobbyItem> items, EventNode<Event> titan) {
        this.titan = titan;
        this.itemsByKey = stampAll(items);
        this.hotbar = hotbarPlacementsOf(this.itemsByKey.values());
        this.dispatcher = EventListener.of(PlayerUseItemEvent.class, this::dispatch);
        this.titan.addListener(this.dispatcher);
    }

    @Override
    public void equip(Player player) {
        player.getInventory().clear();
        this.hotbar.forEach((slot, stack) -> player.getInventory().setItemStack(slot, stack));
    }

    @Override
    public ItemStack stack(String key) {
        LobbyItem item = this.itemsByKey.get(key);
        if (item == null) {
            throw new IllegalArgumentException("No lobby item registered for key '" + key + "'");
        }
        return item.itemStack();
    }

    void stop() {
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

    private static Map<Integer, ItemStack> hotbarPlacementsOf(Collection<LobbyItem> items) {
        Map<Integer, ItemStack> hotbarSlots = new LinkedHashMap<>();
        for (LobbyItem item : items) {
            if (item.placement() instanceof ItemSlot.Hotbar slot) {
                hotbarSlots.put(slot.slot(), item.itemStack());
            }
        }
        return Map.copyOf(hotbarSlots);
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
        Consumer<PlayerUseItemEvent> handler = FeatureNode.guard(item.featureId(), (PlayerUseItemEvent guardedEvent) -> item.onUse().handle(guardedEvent.getPlayer(), guardedEvent));
        handler.accept(event);
    }
}
