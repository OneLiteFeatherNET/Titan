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
package net.onelitefeather.titan.app.helper;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.app.testutils.DummyDeliver;
import net.onelitefeather.titan.common.utils.Items;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MicrotusExtension.class)
class NavigationHelperTest {

    @DisplayName("Test if the NavigationHelper is set with the correct items")
    @Test
    void testNavigationHelperIsItemsSet(Env env) {
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance());

        Instance flatInstance = env.createFlatInstance();
        Player realPlayer = env.createPlayer(flatInstance);
        PlayerInventory realInventory = realPlayer.getInventory();
        Player player = spy(realPlayer);
        doReturn(spy(realInventory)).when(player).getInventory();

        helper.setItems(player);

        verify(player.getInventory(), atLeastOnce()).clear();
        verify(player.getInventory(), atLeastOnce()).setItemStack(4, Items.PLAYER_TELEPORTER);
        verify(player.getInventory(), atLeastOnce()).setEquipment(EquipmentSlot.CHESTPLATE, (byte) EquipmentSlot.CHESTPLATE.armorSlot(), Items.PLAYER_ELYTRA);
    }

    @DisplayName("Test if the NavigationHelper open the navigation gui")
    @Test
    void testNavigationHelperOpenNavigationGui(Env env) {
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance());

        Instance flatInstance = env.createFlatInstance();
        Player realPlayer = env.createPlayer(flatInstance);

        helper.openNavigator(realPlayer);

        Assertions.assertNotNull(realPlayer.getOpenInventory());
    }

    @DisplayName("Test the navigator layout: ElytraRace on 0, Survival on 4, Slender on 5, Creative on 8, blank glass panes on the rest")
    @Test
    void testNavigationHelperLayout(Env env) {
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance());

        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);

        helper.openNavigator(player);
        // Aves computes the per-slot navigator entries asynchronously (scheduled on the next
        // server tick), so the layout is not populated yet when openNavigator() returns.
        env.tick();

        AbstractInventory openInventory = player.getOpenInventory();
        Assertions.assertNotNull(openInventory);
        Assertions.assertInstanceOf(Inventory.class, openInventory);
        Assertions.assertEquals(InventoryType.CHEST_1_ROW, ((Inventory) openInventory).getInventoryType());

        Assertions.assertEquals(Items.NAVIGATOR_ELYTRA_ITEM_STACK, openInventory.getItemStack(0));
        Assertions.assertEquals(Items.NAVIGATOR_SURVIVAL_ITEM_STACK, openInventory.getItemStack(4));
        Assertions.assertEquals(Items.NAVIGATOR_SLENDER_ITEM_STACK, openInventory.getItemStack(5));
        Assertions.assertEquals(Items.NAVIGATOR_CREATIVE_ITEM_STACK, openInventory.getItemStack(8));

        for (int slot : List.of(1, 2, 3, 6, 7)) {
            Assertions.assertEquals(Items.NAVIGATOR_BLANK_ITEM_STACK, openInventory.getItemStack(slot), "Slot " + slot + " should be a blank glass pane");
        }
    }

    @DisplayName("Test if clicking a navigator entry forwards the player through Deliver and closes the click")
    @Test
    void testNavigationHelperClickForwardsThroughDeliver(Env env) {
        Deliver deliver = spy(DummyDeliver.instance());
        NavigationHelper helper = NavigationHelper.instance(deliver);

        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);

        helper.openNavigator(player);
        env.tick();
        AbstractInventory openInventory = player.getOpenInventory();
        Assertions.assertNotNull(openInventory);

        InventoryPreClickEvent clickEvent = new InventoryPreClickEvent(openInventory, player, new Click.Left(4));
        MinecraftServer.getGlobalEventHandler().call(clickEvent);

        verify(deliver, atLeastOnce()).sendPlayer(eq(player), any());
        Assertions.assertTrue(clickEvent.isCancelled(), "The click on a navigator entry should be cancelled so the item stays in place");
    }

}
