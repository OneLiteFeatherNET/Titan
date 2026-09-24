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
package net.onelitefeather.titan.app.feature.protection;

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
import net.onelitefeather.titan.app.module.testing.ModuleHarness;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Exercises {@link ProtectionModule} through a real {@link ModuleHarness}, the way it will run in
 * the lobby: every event it cancels needs a real {@link Player}, so this uses {@link
 * ModuleHarness#start(Env, net.onelitefeather.titan.app.module.LobbyModule...)} rather than the
 * standalone entry point. Ports the behaviour {@code ProtectionListenersTest} pinned down for the
 * old, module-less wiring in {@code Titan#initListeners()}.
 */
@ExtendWith(MicrotusExtension.class)
class ProtectionModuleTest {

    @DisplayName("Picking up an item is cancelled while the module is enabled")
    @Test
    void pickupItemEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);

        try (ModuleHarness harness = ModuleHarness.start(env, new ProtectionModule())) {
            ItemEntity itemEntity = new ItemEntity(ItemStack.of(Material.DIAMOND));
            PickupItemEvent event = new PickupItemEvent(player, itemEntity);
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
        }
    }

    @DisplayName("Clicking in an inventory is cancelled while the module is enabled")
    @Test
    void inventoryPreClickEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);

        try (ModuleHarness harness = ModuleHarness.start(env, new ProtectionModule())) {
            InventoryPreClickEvent event = new InventoryPreClickEvent(player.getInventory(), player, new Click.Left(0));
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
        }
    }

    @DisplayName("Breaking a block is cancelled while the module is enabled")
    @Test
    void playerBlockBreakEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);

        try (ModuleHarness harness = ModuleHarness.start(env, new ProtectionModule())) {
            PlayerBlockBreakEvent event = new PlayerBlockBreakEvent(player, flatInstance, Block.STONE, Block.AIR, new BlockVec(0, 64, 0), BlockFace.TOP);
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
        }
    }

    @DisplayName("Placing a block is cancelled while the module is enabled")
    @Test
    void playerBlockPlaceEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);

        try (ModuleHarness harness = ModuleHarness.start(env, new ProtectionModule())) {
            PlayerBlockPlaceEvent event = new PlayerBlockPlaceEvent(player, flatInstance, Block.STONE, BlockFace.TOP, new BlockVec(0, 64, 0), new BlockVec(0, 64, 0), PlayerHand.MAIN);
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
        }
    }

    @DisplayName("Swapping the main and off hand item is cancelled while the module is enabled")
    @Test
    void playerSwapItemEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);

        try (ModuleHarness harness = ModuleHarness.start(env, new ProtectionModule())) {
            PlayerSwapItemEvent event = new PlayerSwapItemEvent(player, ItemStack.of(Material.DIAMOND), ItemStack.AIR);
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
        }
    }

    @DisplayName("Dropping an item is cancelled while the module is enabled")
    @Test
    void itemDropEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);

        try (ModuleHarness harness = ModuleHarness.start(env, new ProtectionModule())) {
            ItemDropEvent event = new ItemDropEvent(player, ItemStack.of(Material.DIAMOND));
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
        }
    }

    @DisplayName("Once the harness closes, the module's listeners no longer cancel events")
    @Test
    void eventsAreNoLongerCancelledAfterTheHarnessCloses(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        ModuleHarness harness = ModuleHarness.start(env, new ProtectionModule());

        harness.close();

        ItemDropEvent event = new ItemDropEvent(player, ItemStack.of(Material.DIAMOND));
        env.process().eventHandler().call(event);

        Assertions.assertFalse(event.isCancelled(), "no module listener may still be attached once the harness is closed");
    }
}
