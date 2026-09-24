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
import net.minestom.server.entity.EquipmentSlot;
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
 * Cyano/Microtus {@code Env} coverage for {@link ItemRegistry}, for the three scenarios that
 * genuinely need a server: dispatching a used item to its owning module, a look-alike item that was
 * never registered doing nothing, and {@code equip()} against a real {@link Player}. Everything
 * else
 * about {@link ItemRegistry} is covered without a player in {@link ItemRegistryUnitTest}.
 */
@ExtendWith(MicrotusExtension.class)
class ItemRegistryIntegrationTest {

    @DisplayName("Using a registered item's stamped stack reaches its owning module's handler")
    @Test
    void usingARegisteredItemReachesItsOwningModulesHandler(Env env) {
        EventNode<Event> platformNode = EventNode.all("test-item-dispatch");
        ItemRegistry registry = new ItemRegistry(platformNode);
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        List<Player> handledFor = new ArrayList<>();
        ModuleItems items = registry.contextView("navigator", cleanup -> {
        });
        ItemStack stamped = items.register(new LobbyItem(Key.key("titan:navigator"), ItemStack.of(Material.FEATHER), ItemSlot.hotbar(4), (usedBy, event) -> handledFor.add(usedBy)));

        platformNode.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, stamped, System.currentTimeMillis()));

        Assertions.assertEquals(List.of(player), handledFor, "the navigator opens - and only the navigator's own handler runs");
    }

    @DisplayName("A plain feather that was never registered does not reach any handler")
    @Test
    void aPlainFeatherThatWasNeverRegisteredDoesNothing(Env env) {
        EventNode<Event> platformNode = EventNode.all("test-item-dispatch-no-tag");
        ItemRegistry registry = new ItemRegistry(platformNode);
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        List<Player> handledFor = new ArrayList<>();
        registry.contextView("navigator", cleanup -> {
        }).register(new LobbyItem(Key.key("titan:navigator"), ItemStack.of(Material.FEATHER), ItemSlot.hotbar(4), (usedBy, event) -> handledFor.add(usedBy)));

        platformNode.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, ItemStack.of(Material.FEATHER), System.currentTimeMillis()));

        Assertions.assertTrue(handledFor.isEmpty(), "same material, but not the registered stack - the navigator must not open");
    }

    @DisplayName("equip() clears the inventory and sets every placed item; everything else stays empty")
    @Test
    void equipSetsEveryPlacedItemAndClearsEverythingElse(Env env) {
        EventNode<Event> platformNode = EventNode.all("test-item-equip");
        ItemRegistry registry = new ItemRegistry(platformNode);
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        ModuleItems items = registry.contextView("spawn", cleanup -> {
        });
        ItemStack navigatorStack = items.register(new LobbyItem(Key.key("titan:navigator"), ItemStack.of(Material.FEATHER), ItemSlot.hotbar(4), (usedBy, event) -> {
        }));
        ItemStack elytraStack = items.register(new LobbyItem(Key.key("titan:elytra"), ItemStack.of(Material.ELYTRA), ItemSlot.equipment(EquipmentSlot.CHESTPLATE), (usedBy, event) -> {
        }));

        registry.equip(player);

        Assertions.assertEquals(navigatorStack, player.getInventory().getItemStack(4));
        Assertions.assertEquals(elytraStack, player.getEquipment(EquipmentSlot.CHESTPLATE));
        for (int slot = 0; slot < 9; slot++) {
            if (slot == 4) {
                continue;
            }
            Assertions.assertEquals(ItemStack.AIR, player.getInventory().getItemStack(slot), "hotbar slot " + slot + " must be empty");
        }
        Assertions.assertEquals(ItemStack.AIR, player.getEquipment(EquipmentSlot.HELMET));
        Assertions.assertEquals(ItemStack.AIR, player.getEquipment(EquipmentSlot.OFF_HAND));
    }
}
