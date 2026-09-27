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
package net.onelitefeather.titan.app.feature.respawn;

import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerRespawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import net.onelitefeather.titan.app.module.item.ItemSlot;
import net.onelitefeather.titan.app.module.item.LobbyItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Env integration coverage for {@link RespawnModule}, built directly with fakes - see
 * {@code openspec/changes/dissolve-module-platform/tasks.md}, task 2.3: a real death must produce
 * no message and a respawn by the next tick (see the class Javadoc on {@link RespawnModule} for
 * why it cannot be synchronous), and a real respawn must hand the player back exactly the
 * platform's currently registered loadout. Calling {@link RespawnModule#stop()} must leave the
 * player untouched by further events.
 */
@ExtendWith(MicrotusExtension.class)
class RespawnModuleTest {

    private static final Key TEST_ITEM_KEY = Key.key("titan:respawn-module-test-item");

    /**
     * A fresh {@code titan} node attached under the given {@code Env}'s global event handler, and
     * its own, unshared {@link ItemRegistry} - the bridge {@link RespawnModule} still equips
     * through until {@code TODO(dissolve-module-platform, task 3.1)} switches it to the
     * {@code LobbyItems} bean. A new instance per test keeps tests independent (F.I.R.S.T.).
     */
    private static final class TestPlatform implements AutoCloseable {

        private final EventNode<Event> global;
        private final EventNode<Event> titan;
        private final ItemRegistry itemRegistry;

        private TestPlatform(EventNode<Event> global) {
            this.global = global;
            this.titan = EventNode.all("test-titan-" + UUID.randomUUID());
            this.global.addChild(this.titan);
            this.itemRegistry = new ItemRegistry(this.titan);
        }

        static TestPlatform attach(Env env) {
            return new TestPlatform(env.process().eventHandler());
        }

        EventNode<Event> titan() {
            return this.titan;
        }

        ItemRegistry itemRegistry() {
            return this.itemRegistry;
        }

        void registerFeatherItem() {
            this.itemRegistry.contextView("respawn-test-item", cleanup -> {
            }).register(new LobbyItem("respawn-test-item", TEST_ITEM_KEY, ItemStack.of(Material.FEATHER), ItemSlot.hotbar(0), (usedBy, event) -> {
            }));
        }

        @Override
        public void close() {
            this.global.removeChild(this.titan);
        }
    }

    private static RespawnModule startedModule(TestPlatform platform) {
        RespawnModule module = new RespawnModule(platform.titan(), platform.itemRegistry());
        module.start();
        return module;
    }

    @DisplayName("A player's death produces no death message")
    @Test
    void deathProducesNoMessage(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        try (TestPlatform platform = TestPlatform.attach(env)) {
            RespawnModule module = startedModule(platform);
            try {
                Collector<PlayerDeathEvent> collector = env.trackEvent(PlayerDeathEvent.class, EventFilter.PLAYER, player);

                player.kill();

                collector.assertSingle();
                PlayerDeathEvent first = collector.collect().getFirst();
                Assertions.assertEquals(Component.empty(), first.getDeathText());
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A real death triggers a respawn - with the platform's loadout back on - by the next tick")
    @Test
    void deathTriggersARespawnWithLoadoutByTheNextTick(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        try (TestPlatform platform = TestPlatform.attach(env)) {
            platform.registerFeatherItem();
            RespawnModule module = startedModule(platform);
            try {
                Collector<PlayerDeathEvent> deathCollector = env.trackEvent(PlayerDeathEvent.class, EventFilter.PLAYER, player);
                Collector<PlayerRespawnEvent> respawnCollector = env.trackEvent(PlayerRespawnEvent.class, EventFilter.PLAYER, player);

                // Player#kill() dispatches PlayerDeathEvent *before* Player#isDead() flips to
                // true, and Player#respawn() is a no-op while isDead() is still false - so
                // calling respawn() straight from the PlayerDeathEvent listener (the bug this
                // test guards against) would silently do nothing. RespawnModule must defer the
                // respawn to a later tick instead, so right after kill() returns the player must
                // still be dead.
                player.kill();
                Assertions.assertTrue(player.isDead(), "the respawn must not happen synchronously inside the death event - only once a later tick runs");

                // Drives the deferred respawn - scheduled on the player's own per-tick scheduler
                // - to completion. Collector#collect() (used by every assert below) unmaps its
                // underlying listener as a side effect, so it must not be called before the tick
                // that produces the event under test.
                env.tick();

                deathCollector.assertSingle();
                Assertions.assertEquals(Component.empty(), deathCollector.collect().getFirst().getDeathText(), "the death text must be blanked");
                respawnCollector.assertSingle();
                Assertions.assertFalse(player.isDead(), "the player must be alive again after the next tick, without waiting for a respawn screen");
                Assertions.assertEquals(Material.FEATHER, player.getInventory().getItemStack(0).material(), "the registered item must be placed again once the player respawns");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("After a respawn, the player has exactly the items registered with the platform")
    @Test
    void respawnEquipsExactlyTheRegisteredItems(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        try (TestPlatform platform = TestPlatform.attach(env)) {
            platform.registerFeatherItem();
            RespawnModule module = startedModule(platform);
            try {
                env.process().eventHandler().call(new PlayerRespawnEvent(player));

                Assertions.assertEquals(Material.FEATHER, player.getInventory().getItemStack(0).material(), "the registered item must be placed on respawn");
                for (int slot = 1; slot < PlayerInventory.INVENTORY_SIZE; slot++) {
                    Assertions.assertTrue(player.getInventory().getItemStack(slot).isAir(), "slot " + slot + " must be empty after respawn");
                }
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Once stopped, neither death nor respawn is handled any more")
    @Test
    void moduleStopsReactingAfterStop(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        try (TestPlatform platform = TestPlatform.attach(env)) {
            platform.registerFeatherItem();
            RespawnModule module = startedModule(platform);
            module.stop();

            Collector<PlayerDeathEvent> deathCollector = env.trackEvent(PlayerDeathEvent.class, EventFilter.PLAYER, player);
            Collector<PlayerRespawnEvent> respawnCollector = env.trackEvent(PlayerRespawnEvent.class, EventFilter.PLAYER, player);
            player.kill();
            deathCollector.assertSingle();
            PlayerDeathEvent first = deathCollector.collect().getFirst();
            Assertions.assertNotEquals(Component.empty(), first.getDeathText(), "the death text must be untouched once the module is stopped");
            respawnCollector.assertEmpty();
            Assertions.assertTrue(player.isDead(), "the player must stay dead once the module is stopped");

            env.process().eventHandler().call(new PlayerRespawnEvent(player));
            Assertions.assertTrue(player.getInventory().getItemStack(0).isAir(), "equip must not run once the module is stopped");
        }
    }
}
