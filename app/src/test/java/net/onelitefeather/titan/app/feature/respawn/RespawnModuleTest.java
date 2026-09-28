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

import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerRespawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.app.module.item.HotbarLobbyItems;
import net.onelitefeather.titan.app.testutils.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Env integration coverage for {@link RespawnModule}, built directly with fakes: a death produces
 * no message and a respawn by the next tick with the platform's current loadout, and
 * {@link RespawnModule#stop()} leaves the player untouched by further events.
 */
@ExtendWith(MicrotusExtension.class)
class RespawnModuleTest {

    private static final Key TEST_ITEM_KEY = Key.key("titan:respawn-module-test-item");

    private static HotbarLobbyItems noItems(TestTitanNode titan) {
        return new HotbarLobbyItems(List.of(), titan.node());
    }

    private static HotbarLobbyItems featherItem(TestTitanNode titan) {
        LobbyItem feather = new LobbyItem("respawn-test-item", TEST_ITEM_KEY, ItemStack.of(Material.FEATHER), ItemSlot.hotbar(0), (usedBy, event) -> {
        });
        return new HotbarLobbyItems(List.of(feather), titan.node());
    }

    private static RespawnModule startedModule(TestTitanNode titan, HotbarLobbyItems lobbyItems) {
        RespawnModule module = new RespawnModule(titan.node(), lobbyItems);
        module.start();
        return module;
    }

    @DisplayName("A player's death produces no death message")
    @Test
    void deathProducesNoMessage(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            HotbarLobbyItems lobbyItems = noItems(titan);
            RespawnModule module = startedModule(titan, lobbyItems);
            try {
                Collector<PlayerDeathEvent> collector = env.trackEvent(PlayerDeathEvent.class, EventFilter.PLAYER, player);

                player.kill();

                collector.assertSingle();
                PlayerDeathEvent first = collector.collect().getFirst();
                Assertions.assertEquals(Component.empty(), first.getDeathText());
            } finally {
                module.stop();
                lobbyItems.stop();
            }
        }
    }

    @DisplayName("A real death triggers a respawn - with the platform's loadout back on - by the next tick")
    @Test
    void deathTriggersARespawnWithLoadoutByTheNextTick(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            HotbarLobbyItems lobbyItems = featherItem(titan);
            RespawnModule module = startedModule(titan, lobbyItems);
            try {
                Collector<PlayerDeathEvent> deathCollector = env.trackEvent(PlayerDeathEvent.class, EventFilter.PLAYER, player);
                Collector<PlayerRespawnEvent> respawnCollector = env.trackEvent(PlayerRespawnEvent.class, EventFilter.PLAYER, player);

                // kill() dispatches PlayerDeathEvent before isDead() flips true, and respawn() is
                // a no-op while isDead() is false - a listener calling it directly would no-op.
                player.kill();
                Assertions.assertTrue(player.isDead(), "the respawn must not happen synchronously inside the death event - only once a later tick runs");

                // collect() (used below) unmaps its listener as a side effect, so it must run
                // after this tick, which produces the event under test.
                env.tick();

                deathCollector.assertSingle();
                Assertions.assertEquals(Component.empty(), deathCollector.collect().getFirst().getDeathText(), "the death text must be blanked");
                respawnCollector.assertSingle();
                Assertions.assertFalse(player.isDead(), "the player must be alive again after the next tick, without waiting for a respawn screen");
                Assertions.assertEquals(Material.FEATHER, player.getInventory().getItemStack(0).material(), "the registered item must be placed again once the player respawns");
            } finally {
                module.stop();
                lobbyItems.stop();
            }
        }
    }

    @DisplayName("After a respawn, the player has exactly the items registered with the platform")
    @Test
    void respawnEquipsExactlyTheRegisteredItems(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            HotbarLobbyItems lobbyItems = featherItem(titan);
            RespawnModule module = startedModule(titan, lobbyItems);
            try {
                env.process().eventHandler().call(new PlayerRespawnEvent(player));

                Assertions.assertEquals(Material.FEATHER, player.getInventory().getItemStack(0).material(), "the registered item must be placed on respawn");
                for (int slot = 1; slot < PlayerInventory.INVENTORY_SIZE; slot++) {
                    Assertions.assertTrue(player.getInventory().getItemStack(slot).isAir(), "slot " + slot + " must be empty after respawn");
                }
            } finally {
                module.stop();
                lobbyItems.stop();
            }
        }
    }

    @DisplayName("Once stopped, neither death nor respawn is handled any more")
    @Test
    void moduleStopsReactingAfterStop(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            HotbarLobbyItems lobbyItems = featherItem(titan);
            RespawnModule module = startedModule(titan, lobbyItems);
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

            lobbyItems.stop();
        }
    }
}
