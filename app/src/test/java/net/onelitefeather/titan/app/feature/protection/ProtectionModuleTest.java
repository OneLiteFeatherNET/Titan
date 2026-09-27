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

import java.util.UUID;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Exercises {@link ProtectionModule} through direct construction, the way it will run in the
 * lobby: every event it cancels needs a real {@link Player}, so this attaches a fresh {@code titan}
 * node under the given Microtus {@code Env}'s global event handler and starts the module against
 * it, mirroring what {@code PlatformBeans}/Avaje Inject do in production. Ports the behaviour
 * {@code ProtectionListenersTest} pinned down for the old, module-less wiring in
 * {@code Titan#initListeners()}.
 */
@ExtendWith(MicrotusExtension.class)
class ProtectionModuleTest {

    /**
     * Attaches a fresh {@code titan} node under {@code env}'s global event handler, builds and
     * starts a {@link ProtectionModule} against it, and hands both back so a test can tear them
     * down again with {@link #stop(Env, EventNode, ProtectionModule)}.
     */
    private record Fixture(EventNode<Event> titan, ProtectionModule module) {

        static Fixture start(Env env) {
            EventNode<Event> titan = EventNode.all("test-titan-" + UUID.randomUUID());
            env.process().eventHandler().addChild(titan);
            ProtectionModule module = new ProtectionModule(titan);
            module.start();
            return new Fixture(titan, module);
        }
    }

    private static void stop(Env env, Fixture fixture) {
        fixture.module().stop();
        env.process().eventHandler().removeChild(fixture.titan());
    }

    @DisplayName("Picking up an item is cancelled while the module is started")
    @Test
    void pickupItemEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        Fixture fixture = Fixture.start(env);

        try {
            ItemEntity itemEntity = new ItemEntity(ItemStack.of(Material.DIAMOND));
            PickupItemEvent event = new PickupItemEvent(player, itemEntity);
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
        } finally {
            stop(env, fixture);
        }
    }

    @DisplayName("Clicking in an inventory is cancelled while the module is started")
    @Test
    void inventoryPreClickEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        Fixture fixture = Fixture.start(env);

        try {
            InventoryPreClickEvent event = new InventoryPreClickEvent(player.getInventory(), player, new Click.Left(0));
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
        } finally {
            stop(env, fixture);
        }
    }

    @DisplayName("Breaking a block is cancelled while the module is started")
    @Test
    void playerBlockBreakEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        Fixture fixture = Fixture.start(env);

        try {
            PlayerBlockBreakEvent event = new PlayerBlockBreakEvent(player, flatInstance, Block.STONE, Block.AIR, new BlockVec(0, 64, 0), BlockFace.TOP);
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
        } finally {
            stop(env, fixture);
        }
    }

    @DisplayName("Placing a block is cancelled while the module is started")
    @Test
    void playerBlockPlaceEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        Fixture fixture = Fixture.start(env);

        try {
            PlayerBlockPlaceEvent event = new PlayerBlockPlaceEvent(player, flatInstance, Block.STONE, BlockFace.TOP, new BlockVec(0, 64, 0), new BlockVec(0, 64, 0), PlayerHand.MAIN);
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
        } finally {
            stop(env, fixture);
        }
    }

    @DisplayName("Swapping the main and off hand item is cancelled while the module is started")
    @Test
    void playerSwapItemEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        Fixture fixture = Fixture.start(env);

        try {
            PlayerSwapItemEvent event = new PlayerSwapItemEvent(player, ItemStack.of(Material.DIAMOND), ItemStack.AIR);
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
        } finally {
            stop(env, fixture);
        }
    }

    @DisplayName("Dropping an item is cancelled while the module is started")
    @Test
    void itemDropEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        Fixture fixture = Fixture.start(env);

        try {
            ItemDropEvent event = new ItemDropEvent(player, ItemStack.of(Material.DIAMOND));
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
        } finally {
            stop(env, fixture);
        }
    }

    @DisplayName("Once the module is stopped, none of its events are cancelled anymore")
    @Test
    void eventsAreNoLongerCancelledAfterStop(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        Fixture fixture = Fixture.start(env);

        stop(env, fixture);

        ItemDropEvent event = new ItemDropEvent(player, ItemStack.of(Material.DIAMOND));
        env.process().eventHandler().call(event);

        Assertions.assertFalse(event.isCancelled(), "no feature listener may still be attached once the module is stopped");
    }
}
