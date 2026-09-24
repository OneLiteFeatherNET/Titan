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
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;
import net.onelitefeather.titan.app.module.item.ItemSlot;
import net.onelitefeather.titan.app.module.item.LobbyItem;
import net.onelitefeather.titan.app.module.testing.ModuleHarness;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Env integration coverage for {@link RespawnModule}: a real death must produce no message and an
 * immediate respawn, and a real respawn must hand the player back exactly the platform's currently
 * registered loadout. Closing the harness must leave the player untouched by further events.
 */
@ExtendWith(MicrotusExtension.class)
class RespawnModuleTest {

    private static final Key TEST_ITEM_KEY = Key.key("titan:respawn-module-test-item");

    /**
     * Registers one hotbar-placed {@link LobbyItem}, so a test can observe {@code items().equip}.
     */
    private static final class ItemRegisteringModule implements LobbyModule {

        @Override
        public String id() {
            return "respawn-test-item";
        }

        @Override
        public void enable(ModuleContext context) {
            context.items().register(new LobbyItem(TEST_ITEM_KEY, ItemStack.of(Material.FEATHER), ItemSlot.hotbar(0), (usedBy, event) -> {
            }));
        }
    }

    @DisplayName("A player's death produces no death message")
    @Test
    void deathProducesNoMessage(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        try (ModuleHarness harness = ModuleHarness.start(env, new RespawnModule())) {
            Collector<PlayerDeathEvent> collector = env.trackEvent(PlayerDeathEvent.class, EventFilter.PLAYER, player);

            player.kill();

            collector.assertSingle();
            PlayerDeathEvent first = collector.collect().getFirst();
            Assertions.assertEquals(Component.empty(), first.getDeathText());
        }
    }

    @DisplayName("A player's death triggers an immediate respawn")
    @Test
    void deathTriggersAnImmediateRespawn(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = spy(env.createPlayer(instance));

        try (ModuleHarness harness = ModuleHarness.start(env, new RespawnModule())) {
            Collector<PlayerDeathEvent> collector = env.trackEvent(PlayerDeathEvent.class, EventFilter.PLAYER, player);

            player.kill();

            collector.assertSingle();
            verify(player).respawn();
        }
    }

    @DisplayName("After a respawn, the player has exactly the items registered with the platform")
    @Test
    void respawnEquipsExactlyTheRegisteredItems(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        try (ModuleHarness harness = ModuleHarness.start(env, new RespawnModule(), new ItemRegisteringModule())) {
            env.process().eventHandler().call(new PlayerRespawnEvent(player));

            Assertions.assertEquals(Material.FEATHER, player.getInventory().getItemStack(0).material(), "the registered item must be placed on respawn");
            for (int slot = 1; slot < PlayerInventory.INVENTORY_SIZE; slot++) {
                Assertions.assertTrue(player.getInventory().getItemStack(slot).isAir(), "slot " + slot + " must be empty after respawn");
            }
        }
    }

    @DisplayName("Once the harness is closed, neither death nor respawn is handled any more")
    @Test
    void moduleStopsReactingAfterHarnessCloses(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = spy(env.createPlayer(instance));
        ModuleHarness harness = ModuleHarness.start(env, new RespawnModule(), new ItemRegisteringModule());
        harness.close();

        Collector<PlayerDeathEvent> collector = env.trackEvent(PlayerDeathEvent.class, EventFilter.PLAYER, player);
        player.kill();
        collector.assertSingle();
        PlayerDeathEvent first = collector.collect().getFirst();
        Assertions.assertNotEquals(Component.empty(), first.getDeathText(), "the death text must be untouched once the module is closed");
        verify(player, times(0)).respawn();

        env.process().eventHandler().call(new PlayerRespawnEvent(player));
        Assertions.assertTrue(player.getInventory().getItemStack(0).isAir(), "equip must not run once the module is closed");
    }
}
