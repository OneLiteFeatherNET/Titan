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
package net.onelitefeather.titan.app.feature.elytra;

import java.util.List;
import java.util.UUID;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.Scheduler;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.feature.navigator.NavigatorItems;
import net.onelitefeather.titan.app.feature.navigator.NavigatorModule;
import net.onelitefeather.titan.app.module.item.LobbyItem;
import net.onelitefeather.titan.app.module.item.LobbyItems;
import net.onelitefeather.titan.app.testutils.DummyDeliver;
import net.onelitefeather.titan.common.feature.FeatureFlags;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Cross-feature coverage for the {@code lobby-hotbar} spec's "Standardausstattung": once
 * {@link NavigatorModule} and {@link ElytraModule} are both bean-based features contributing their
 * items through {@link LobbyItems}, a joining player must still end up with exactly the feather in
 * hotbar slot 4 and the elytra on the chestplate - nothing else - and using the feather must still
 * open the (single, shared) navigator inventory.
 *
 * <p>Builds both features directly on one shared {@code titan} node, the way {@code PlatformBeans}
 * wires them in production, without a running {@code BeanScope} - see
 * {@code openspec/changes/dissolve-module-platform/tasks.md}, task 2.7's "mixed state" note: bean
 * items are dispatched only through {@link LobbyItems}, and equipping still yields the same lobby
 * loadout as before this change.
 */
@ExtendWith(MicrotusExtension.class)
class StandardLoadoutTest {

    private static AbstractInventory openNavigatorFeather(Env env, Player player) {
        ItemStack feather = player.getInventory().getItemStack(4);
        env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, feather, 0L));
        return player.getOpenInventory();
    }

    @DisplayName("A joining player gets exactly the feather (slot 4) and the elytra (chestplate), and the feather opens the navigator")
    @Test
    void standardLoadoutHoldsAndTheFeatherOpensTheNavigator(Env env) {
        EventNode<Event> titan = EventNode.all("test-titan-" + UUID.randomUUID());
        env.process().eventHandler().addChild(titan);

        FeatureFlags alwaysActive = new FeatureFlags() {
            @Override
            public boolean exists(String featureName) {
                return true;
            }

            @Override
            public boolean isActive(String featureName) {
                return true;
            }
        };
        NavigatorModule navigator = new NavigatorModule(titan, DummyDeliver.instance(), alwaysActive);
        FireworkBoostTracker boosts = new FireworkBoostTracker();
        Scheduler scheduler = env.process().scheduler();
        // ElytraModule needs LobbyItems before it can start (its offhand listener reads the
        // stamped firework stack from it), so LobbyItems is built first from both features' real
        // items - mirroring the order Avaje itself must use, since LobbyItems depends on the item
        // beans, not on ElytraModule.
        LobbyItem navigatorFeather = new NavigatorItems().navigatorFeather(navigator);
        ElytraLobbyItems elytraItems = new ElytraLobbyItems();
        LobbyItem elytraChestplate = elytraItems.elytraChestplate();
        LobbyItem firework = elytraItems.firework(boosts);
        LobbyItems lobbyItems = new LobbyItems(List.of(navigatorFeather, elytraChestplate, firework), titan);
        ElytraModule elytra = new ElytraModule(titan, lobbyItems, boosts, scheduler);

        navigator.start();
        elytra.start();
        try {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);

            lobbyItems.equip(player);

            Assertions.assertEquals(Material.FEATHER, player.getInventory().getItemStack(4).material(), "hotbar slot 4 must hold the navigator feather");
            for (int slot = 0; slot < 9; slot++) {
                if (slot == 4) {
                    continue;
                }
                Assertions.assertEquals(ItemStack.AIR, player.getInventory().getItemStack(slot), "hotbar slot " + slot + " must be empty");
            }
            Assertions.assertEquals(Material.ELYTRA, player.getEquipment(EquipmentSlot.CHESTPLATE).material(), "the chestplate slot must hold the elytra");
            Assertions.assertEquals(ItemStack.AIR, player.getEquipment(EquipmentSlot.HELMET));
            Assertions.assertEquals(ItemStack.AIR, player.getEquipment(EquipmentSlot.LEGGINGS));
            Assertions.assertEquals(ItemStack.AIR, player.getEquipment(EquipmentSlot.BOOTS));
            Assertions.assertEquals(ItemStack.AIR, player.getEquipment(EquipmentSlot.OFF_HAND));

            AbstractInventory openInventory = openNavigatorFeather(env, player);
            Assertions.assertNotNull(openInventory, "using the feather must open the navigator");
            Assertions.assertEquals(InventoryType.CHEST_1_ROW, ((Inventory) openInventory).getInventoryType());
            Assertions.assertEquals(Material.ELYTRA, openInventory.getItemStack(0).material(), "the opened inventory must be the real navigator layout");

            player.closeInventory();
            AbstractInventory reopened = openNavigatorFeather(env, player);
            Assertions.assertSame(openInventory, reopened, "the feather must always open the one shared navigator inventory, never a new one");
        } finally {
            elytra.stop();
            navigator.stop();
            lobbyItems.stop();
            env.process().eventHandler().removeChild(titan);
        }
    }
}
