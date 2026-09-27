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

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Coverage for the transition bridge {@code PlatformBeans.itemRegistry()} sets up between the old
 * {@link ItemRegistry} and the new {@link LobbyItems} (see
 * {@code openspec/changes/dissolve-module-platform/design.md}, decision 2, and
 * {@link ItemRegistry#registerBridged(List)}): a bean-provided {@link LobbyItem} is registered with
 * both on the same event node, but a use must reach its handler exactly once - {@link LobbyItems}
 * is
 * the single owner of {@code onUse} for bean items, {@link ItemRegistry}'s own dispatcher must skip
 * a bridged registration - while the old {@link ItemRegistry#equip(Player)} must still place it,
 * since a not-yet-migrated module still equips through it.
 *
 * <p>TODO(dissolve-module-platform, task 3.1): delete this test alongside {@link ItemRegistry} and
 * {@link ItemRegistry#registerBridged(List)}.
 */
@ExtendWith(MicrotusExtension.class)
class BridgedItemDispatchTest {

    @DisplayName("A bean item bridged into the old registry is dispatched exactly once, by LobbyItems")
    @Test
    void aBridgedBeanItemIsDispatchedExactlyOnceByLobbyItems(Env env) {
        EventNode<Event> titan = EventNode.all("test-bridge-dispatch");
        List<Player> handledFor = new ArrayList<>();
        LobbyItem navigator = new LobbyItem("navigator", Key.key("titan:navigator"), ItemStack.of(Material.FEATHER), ItemSlot.hotbar(4), (usedBy, event) -> handledFor.add(usedBy));
        ItemRegistry registry = new ItemRegistry(titan);
        registry.registerBridged(List.of(navigator));
        LobbyItems lobbyItems = new LobbyItems(List.of(navigator), titan);
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        ItemStack stamped = lobbyItems.stack(Key.key("titan:navigator"));

        titan.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, stamped, 0L));

        Assertions.assertEquals(List.of(player), handledFor, "the handler must run exactly once, not once per dispatcher");
    }

    @DisplayName("The old registry's equip() still places a bridged item")
    @Test
    void theOldRegistrysEquipStillPlacesABridgedItem(Env env) {
        EventNode<Event> titan = EventNode.all("test-bridge-equip");
        LobbyItem navigator = new LobbyItem("navigator", Key.key("titan:navigator"), ItemStack.of(Material.FEATHER), ItemSlot.hotbar(4), (usedBy, event) -> {
        });
        ItemRegistry registry = new ItemRegistry(titan);
        registry.registerBridged(List.of(navigator));
        new LobbyItems(List.of(navigator), titan);
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        registry.equip(player);

        Assertions.assertEquals(Material.FEATHER, player.getInventory().getItemStack(4).material(), "equip() must still place the bridged item");
    }
}
