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
package net.onelitefeather.titan.app.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.utils.Cancelable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Characterizes the lobby's protection behaviour. {@code Titan#initListeners()} does not use a
 * dedicated listener class for this: it wires each of these six event types straight to
 * {@link Cancelable#cancel} on the shared event node. These tests pin that behaviour down before it
 * moves into a {@code feature/protection} module.
 */
@ExtendWith(MicrotusExtension.class)
class ProtectionListenersTest {

    @DisplayName("Test if picking up an item is cancelled")
    @Test
    void testPickupItemEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        MinecraftServer.getGlobalEventHandler().addListener(PickupItemEvent.class, Cancelable::cancel);

        ItemEntity itemEntity = new ItemEntity(ItemStack.of(Material.DIAMOND));
        PickupItemEvent event = new PickupItemEvent(player, itemEntity);
        MinecraftServer.getGlobalEventHandler().call(event);

        Assertions.assertTrue(event.isCancelled());
    }

    @DisplayName("Test if clicking in an inventory is cancelled")
    @Test
    void testInventoryPreClickEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        MinecraftServer.getGlobalEventHandler().addListener(InventoryPreClickEvent.class, Cancelable::cancel);

        InventoryPreClickEvent event = new InventoryPreClickEvent(player.getInventory(), player, new Click.Left(0));
        MinecraftServer.getGlobalEventHandler().call(event);

        Assertions.assertTrue(event.isCancelled());
    }

    @DisplayName("Test if breaking a block is cancelled")
    @Test
    void testPlayerBlockBreakEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockBreakEvent.class, Cancelable::cancel);

        PlayerBlockBreakEvent event = new PlayerBlockBreakEvent(player, flatInstance, Block.STONE, Block.AIR,
                new BlockVec(0, 64, 0), BlockFace.TOP);
        MinecraftServer.getGlobalEventHandler().call(event);

        Assertions.assertTrue(event.isCancelled());
    }

    @DisplayName("Test if placing a block is cancelled")
    @Test
    void testPlayerBlockPlaceEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockPlaceEvent.class, Cancelable::cancel);

        PlayerBlockPlaceEvent event = new PlayerBlockPlaceEvent(player, flatInstance, Block.STONE, BlockFace.TOP,
                new BlockVec(0, 64, 0), new BlockVec(0, 64, 0), PlayerHand.MAIN);
        MinecraftServer.getGlobalEventHandler().call(event);

        Assertions.assertTrue(event.isCancelled());
    }

    @DisplayName("Test if swapping the main and off hand item is cancelled")
    @Test
    void testPlayerSwapItemEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        MinecraftServer.getGlobalEventHandler().addListener(PlayerSwapItemEvent.class, Cancelable::cancel);

        PlayerSwapItemEvent event = new PlayerSwapItemEvent(player, ItemStack.of(Material.DIAMOND), ItemStack.AIR);
        MinecraftServer.getGlobalEventHandler().call(event);

        Assertions.assertTrue(event.isCancelled());
    }

    @DisplayName("Test if dropping an item is cancelled")
    @Test
    void testItemDropEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        MinecraftServer.getGlobalEventHandler().addListener(ItemDropEvent.class, Cancelable::cancel);

        ItemDropEvent event = new ItemDropEvent(player, ItemStack.of(Material.DIAMOND));
        MinecraftServer.getGlobalEventHandler().call(event);

        Assertions.assertTrue(event.isCancelled());
    }
}
