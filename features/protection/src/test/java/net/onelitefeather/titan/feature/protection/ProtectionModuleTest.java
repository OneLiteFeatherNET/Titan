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
package net.onelitefeather.titan.feature.protection;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
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
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Exercises {@link ProtectionModule} through direct construction, the way it will run in the
 * lobby: every event it cancels needs a real {@link Player}, so this attaches a fresh {@code titan}
 * node under the given Microtus {@code Env}'s global event handler and starts the module against
 * it, mirroring what {@code PlatformBeans}/Avaje Inject do in production.
 */
@ExtendWith(MicrotusExtension.class)
class ProtectionModuleTest {

    private static final AttributeKey<String> EVENT = AttributeKey.stringKey("event");
    private static final String DENIED = "protection.denied";

    /**
     * A started {@link ProtectionModule} plus its {@code titan} node and its own telemetry, torn
     * down via {@link #stop(Fixture)}. Counters are read before the stop, since closing the
     * telemetry ends its metric reader.
     */
    private record Fixture(TestTitanNode titan, ProtectionModule module, TestTelemetry telemetry) {

        static Fixture start(Env env) {
            TestTitanNode titan = TestTitanNode.attach(env);
            TestTelemetry telemetry = TestTelemetry.create();
            ProtectionModule module = new ProtectionModule(titan.node(), telemetry.telemetry());
            module.start();
            return new Fixture(titan, module, telemetry);
        }
    }

    private static void stop(Fixture fixture) {
        fixture.module().stop();
        fixture.titan().close();
        fixture.telemetry().close();
    }

    private static Attributes denialOf(String event) {
        return Attributes.of(EVENT, event);
    }

    @DisplayName("Picking up an item is cancelled and counted as pickup while the module is started")
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
            Assertions.assertEquals(1L, fixture.telemetry().counter(DENIED, denialOf("pickup")), "pickup denials");
        } finally {
            stop(fixture);
        }
    }

    @DisplayName("Clicking in an inventory is cancelled and counted as inventory_click while the module is started")
    @Test
    void inventoryPreClickEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        Fixture fixture = Fixture.start(env);

        try {
            InventoryPreClickEvent event = new InventoryPreClickEvent(player.getInventory(), player, new Click.Left(0));
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
            Assertions.assertEquals(1L, fixture.telemetry().counter(DENIED, denialOf("inventory_click")), "inventory click denials");
        } finally {
            stop(fixture);
        }
    }

    @DisplayName("Breaking a block is cancelled and counted as block_break while the module is started")
    @Test
    void playerBlockBreakEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        Fixture fixture = Fixture.start(env);

        try {
            PlayerBlockBreakEvent event = new PlayerBlockBreakEvent(player, flatInstance, Block.STONE, Block.AIR, new BlockVec(0, 64, 0), BlockFace.TOP);
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
            Assertions.assertEquals(1L, fixture.telemetry().counter(DENIED, denialOf("block_break")), "block break denials");
        } finally {
            stop(fixture);
        }
    }

    @DisplayName("Placing a block is cancelled and counted as block_place while the module is started")
    @Test
    void playerBlockPlaceEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        Fixture fixture = Fixture.start(env);

        try {
            PlayerBlockPlaceEvent event = new PlayerBlockPlaceEvent(player, flatInstance, Block.STONE, BlockFace.TOP, new BlockVec(0, 64, 0), new BlockVec(0, 64, 0), PlayerHand.MAIN);
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
            Assertions.assertEquals(1L, fixture.telemetry().counter(DENIED, denialOf("block_place")), "block place denials");
        } finally {
            stop(fixture);
        }
    }

    @DisplayName("Swapping the main and off hand item is cancelled and counted as item_swap while the module is started")
    @Test
    void playerSwapItemEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        Fixture fixture = Fixture.start(env);

        try {
            PlayerSwapItemEvent event = new PlayerSwapItemEvent(player, ItemStack.of(Material.DIAMOND), ItemStack.AIR);
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
            Assertions.assertEquals(1L, fixture.telemetry().counter(DENIED, denialOf("item_swap")), "item swap denials");
        } finally {
            stop(fixture);
        }
    }

    @DisplayName("Dropping an item is cancelled and counted as item_drop while the module is started")
    @Test
    void itemDropEventIsCancelled(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        Fixture fixture = Fixture.start(env);

        try {
            ItemDropEvent event = new ItemDropEvent(player, ItemStack.of(Material.DIAMOND));
            env.process().eventHandler().call(event);

            Assertions.assertTrue(event.isCancelled());
            Assertions.assertEquals(1L, fixture.telemetry().counter(DENIED, denialOf("item_drop")), "item drop denials");
        } finally {
            stop(fixture);
        }
    }

    @DisplayName("A thousand denied events produce no span and no attribute beyond the event name")
    @Test
    void deniedEventsAreCountedWithoutSpans(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        Fixture fixture = Fixture.start(env);

        try {
            for (int i = 0; i < 1000; i++) {
                env.process().eventHandler().call(new ItemDropEvent(player, ItemStack.of(Material.DIAMOND)));
            }

            Assertions.assertTrue(fixture.telemetry().spans().isEmpty(), "high-frequency denials must not open spans, got: " + fixture.telemetry().spans());
            Assertions.assertFalse(fixture.telemetry().allSpanText().contains(player.getUsername()), "no span may carry the player's name");
            Assertions.assertEquals(1000L, fixture.telemetry().counter(DENIED, denialOf("item_drop")), "every denial is counted, under the event name only");
        } finally {
            stop(fixture);
        }
    }

    @DisplayName("Once the module is stopped, none of its events are cancelled anymore")
    @Test
    void eventsAreNoLongerCancelledAfterStop(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        Fixture fixture = Fixture.start(env);

        stop(fixture);

        ItemDropEvent event = new ItemDropEvent(player, ItemStack.of(Material.DIAMOND));
        env.process().eventHandler().call(event);

        Assertions.assertFalse(event.isCancelled(), "no feature listener may still be attached once the module is stopped");
    }
}
