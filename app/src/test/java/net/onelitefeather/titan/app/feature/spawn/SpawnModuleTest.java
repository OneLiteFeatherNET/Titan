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
package net.onelitefeather.titan.app.feature.spawn;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.UpdateSimulationDistancePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;
import net.onelitefeather.titan.app.module.item.ItemSlot;
import net.onelitefeather.titan.app.module.item.LobbyItem;
import net.onelitefeather.titan.app.module.testing.ModuleHarness;
import net.onelitefeather.titan.common.config.ConfigValues;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code Env} (Cyano/Microtus) coverage for {@link SpawnModule}, exercising it through
 * {@link ModuleHarness} exactly the way a real
 * {@link net.onelitefeather.titan.app.module.ModuleRegistry}
 * would start it. Covers the {@code lobby-modules}/{@code lobby-hotbar} scenarios task 6.2 carries:
 * spawning instance and respawn point on configuration, teleport plus simulation distance plus
 * equipment on spawn, and the height-bounds teleport.
 */
@ExtendWith(MicrotusExtension.class)
class SpawnModuleTest {

    /**
     * The shipped {@code spawn} defaults, read from the facade rather than hardcoded, so a changed
     * shipped default (see {@code application.yaml}) cannot silently desync this test from
     * production - read-only, never mutated (F.I.R.S.T. - Independent).
     */
    private static final int MIN_HEIGHT = ConfigValues.intValue(SpawnSettings.MIN_HEIGHT_KEY);
    private static final int MAX_HEIGHT = ConfigValues.intValue(SpawnSettings.MAX_HEIGHT_KEY);
    private static final int SIMULATION_DISTANCE = ConfigValues.intValue(SpawnSettings.SIMULATION_DISTANCE_KEY);

    /** Registers one hotbar item so {@code items().equip(player)} has something to observe. */
    private static final class DummyItemModule implements LobbyModule {

        @Override
        public String id() {
            return "dummy-item";
        }

        @Override
        public void enable(ModuleContext context) {
            context.items().register(new LobbyItem(Key.key("titan:test-dummy"), ItemStack.of(Material.STICK), ItemSlot.hotbar(0), (player, event) -> {
            }));
        }
    }

    @DisplayName("The configuration event sets the spawning instance and the player's respawn point")
    @Test
    void configurationEventSetsSpawningInstanceAndRespawnPoint(Env env) throws InterruptedException {
        Instance targetInstance = env.createFlatInstance();
        Pos spawnPos = new Pos(1, 2, 3);
        SpawnModule module = new SpawnModule(targetInstance, () -> spawnPos);

        try (ModuleHarness harness = ModuleHarness.start(env, module)) {
            Player player = env.createPlayer(targetInstance);
            AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);

            // AsyncPlayerConfigurationEvent is an AsyncEvent and must be called from a virtual
            // thread; join it so the assertions below only run once the listener has finished.
            Thread callingThread = Thread.startVirtualThread(() -> env.process().eventHandler().call(event));
            callingThread.join();

            Assertions.assertEquals(targetInstance, event.getSpawningInstance());
            Assertions.assertEquals(spawnPos, player.getRespawnPoint());
        }
    }

    @DisplayName("Spawning teleports the player, sends the simulation distance and equips the standard loadout")
    @Test
    void spawnTeleportsSendsSimulationDistanceAndAppliesEquipment(Env env) {
        Instance instance = env.createFlatInstance();
        Pos spawnPos = new Pos(5, 64, 5);
        SpawnModule module = new SpawnModule(instance, () -> spawnPos);

        try (ModuleHarness harness = ModuleHarness.start(env, module, new DummyItemModule())) {
            TestConnection connection = env.createConnection();
            Player player = connection.connect(instance);
            Collector<UpdateSimulationDistancePacket> collector = connection.trackIncoming(UpdateSimulationDistancePacket.class);

            env.process().eventHandler().call(new PlayerSpawnEvent(player, instance, true));

            collector.assertSingle();
            Assertions.assertEquals(SIMULATION_DISTANCE, collector.collect().getFirst().simulationDistance());
            Assertions.assertEquals(spawnPos, player.getPosition());
            Assertions.assertEquals(Material.STICK, player.getInventory().getItemStack(0).material(), "equip() must have applied the other module's registered item too");
        }
    }

    @DisplayName("Falling below the configured minimum height teleports the player back to spawn")
    @Test
    void fallingBelowMinHeightTeleportsToSpawn(Env env) {
        Instance instance = env.createFlatInstance();
        Pos spawnPos = new Pos(10, 100, 10);
        SpawnModule module = new SpawnModule(instance, () -> spawnPos);

        try (ModuleHarness harness = ModuleHarness.start(env, module)) {
            Player player = env.createPlayer(instance);
            Pos belowMin = new Pos(0, MIN_HEIGHT - 10, 0);
            player.teleport(belowMin);

            env.process().eventHandler().call(new PlayerMoveEvent(player, belowMin, true));

            Assertions.assertEquals(spawnPos, player.getPosition());
        }
    }

    @DisplayName("Rising above the configured maximum height teleports the player back to spawn")
    @Test
    void risingAboveMaxHeightTeleportsToSpawn(Env env) {
        Instance instance = env.createFlatInstance();
        Pos spawnPos = new Pos(10, 100, 10);
        SpawnModule module = new SpawnModule(instance, () -> spawnPos);

        try (ModuleHarness harness = ModuleHarness.start(env, module)) {
            Player player = env.createPlayer(instance);
            Pos aboveMax = new Pos(0, MAX_HEIGHT + 10, 0);
            player.teleport(aboveMax);

            env.process().eventHandler().call(new PlayerMoveEvent(player, aboveMax, true));

            Assertions.assertEquals(spawnPos, player.getPosition());
        }
    }

    @DisplayName("Staying within the configured height bounds does not teleport the player")
    @Test
    void withinHeightBoundsDoesNotTeleport(Env env) {
        Instance instance = env.createFlatInstance();
        Pos spawnPos = new Pos(10, 100, 10);
        SpawnModule module = new SpawnModule(instance, () -> spawnPos);

        try (ModuleHarness harness = ModuleHarness.start(env, module)) {
            Player player = env.createPlayer(instance);
            Pos withinBounds = new Pos(0, (MIN_HEIGHT + MAX_HEIGHT) / 2.0, 0);
            player.teleport(withinBounds);

            env.process().eventHandler().call(new PlayerMoveEvent(player, withinBounds, true));

            Assertions.assertEquals(withinBounds, player.getPosition());
        }
    }
}
