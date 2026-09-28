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
package net.onelitefeather.titan.app.bootstrap;

import io.avaje.inject.BeanScope;
import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.feature.elytra.ElytraModule;
import net.onelitefeather.titan.feature.navigator.NavigatorModule;
import net.onelitefeather.titan.core.module.item.LobbyItems;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * Cross-feature coverage for a joining player's standard loadout: exactly the feather in hotbar
 * slot 4 and the elytra on the chestplate, and using the feather opens the shared navigator
 * inventory, driven through the real {@link BeanScope} since neither module's lifecycle is
 * reachable from this package.
 *
 * <p>The scope is bound to {@code env}'s own instance because the real {@code SpawnModule} would
 * otherwise redirect the joining player to {@code PlatformBeans}' own, ungenerated
 * {@link InstanceContainer}, hanging {@link Env#createPlayer} forever waiting for chunks that
 * never load.
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
    @Timeout(30)
    void standardLoadoutHoldsAndTheFeatherOpensTheNavigator(Env env) {
        Instance instance = env.createFlatInstance();
        BeanScope scope = BeanScope.builder().forTesting().mock(FeatureFlags.class).mock(MapProvider.class, mapProvider -> Mockito.when(mapProvider.getActiveLobby()).thenReturn(new LobbyMap("test", new Pos(0, 65, 0), List.of()))).bean(InstanceContainer.class, (InstanceContainer) instance).bean(Instance.class, instance).build();

        try {
            Assertions.assertNotNull(scope.get(NavigatorModule.class));
            Assertions.assertNotNull(scope.get(ElytraModule.class));
            LobbyItems lobbyItems = scope.get(LobbyItems.class);

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
            scope.close();
        }
    }
}
