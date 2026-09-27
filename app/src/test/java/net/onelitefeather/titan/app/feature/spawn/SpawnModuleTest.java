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

import io.avaje.config.Config;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
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
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import net.onelitefeather.titan.app.module.item.ItemSlot;
import net.onelitefeather.titan.app.module.item.LobbyItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code Env} (Cyano/Microtus) coverage for {@link SpawnModule}, built directly with fakes - see
 * {@code openspec/changes/dissolve-module-platform/tasks.md}, task 2.2. Covers the
 * {@code lobby-modules}/{@code lobby-hotbar} scenarios task 6.2 carries: spawning instance and
 * respawn point on configuration, teleport plus simulation distance plus equipment on spawn, and
 * the height-bounds teleport - plus that {@link SpawnModule#stop()} leaves no listener behind.
 */
@ExtendWith(MicrotusExtension.class)
class SpawnModuleTest {

    /**
     * The shipped {@code spawn} defaults, read from the facade rather than hardcoded, so a changed
     * shipped default (see {@code application.yaml}) cannot silently desync this test from
     * production - read-only, never mutated (F.I.R.S.T. - Independent).
     */
    private static final int MIN_HEIGHT = Config.getAs(SpawnSettings.MIN_HEIGHT_KEY, Integer::parseInt);
    private static final int MAX_HEIGHT = Config.getAs(SpawnSettings.MAX_HEIGHT_KEY, Integer::parseInt);
    private static final int SIMULATION_DISTANCE = Config.getAs(SpawnSettings.SIMULATION_DISTANCE_KEY, Integer::parseInt);

    /**
     * A fresh {@code titan} node attached under the given {@code Env}'s global event handler, and
     * its own, unshared {@link ItemRegistry} - the bridge {@link SpawnModule} still equips through
     * until {@code TODO(dissolve-module-platform, task 3.1)} switches it to the {@code LobbyItems}
     * bean. A new instance per test keeps tests independent (F.I.R.S.T.).
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

        @Override
        public void close() {
            this.global.removeChild(this.titan);
        }
    }

    @DisplayName("The configuration event sets the spawning instance and the player's respawn point")
    @Test
    void configurationEventSetsSpawningInstanceAndRespawnPoint(Env env) throws InterruptedException {
        Instance targetInstance = env.createFlatInstance();
        Pos spawnPos = new Pos(1, 2, 3);

        try (TestPlatform platform = TestPlatform.attach(env)) {
            SpawnModule module = new SpawnModule(targetInstance, () -> spawnPos, platform.titan(), platform.itemRegistry());
            module.start();
            try {
                Player player = env.createPlayer(targetInstance);
                AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);

                // AsyncPlayerConfigurationEvent is an AsyncEvent and must be called from a virtual
                // thread; join it so the assertions below only run once the listener has finished.
                Thread callingThread = Thread.startVirtualThread(() -> env.process().eventHandler().call(event));
                callingThread.join();

                Assertions.assertEquals(targetInstance, event.getSpawningInstance());
                Assertions.assertEquals(spawnPos, player.getRespawnPoint());
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Spawning teleports the player, sends the simulation distance and equips the standard loadout")
    @Test
    void spawnTeleportsSendsSimulationDistanceAndAppliesEquipment(Env env) {
        Instance instance = env.createFlatInstance();
        Pos spawnPos = new Pos(5, 64, 5);

        try (TestPlatform platform = TestPlatform.attach(env)) {
            platform.itemRegistry().contextView("dummy-item", cleanup -> {
            }).register(new LobbyItem("dummy-item", Key.key("titan:test-dummy"), ItemStack.of(Material.STICK), ItemSlot.hotbar(0), (player, event) -> {
            }));
            SpawnModule module = new SpawnModule(instance, () -> spawnPos, platform.titan(), platform.itemRegistry());
            module.start();
            try {
                TestConnection connection = env.createConnection();
                Player player = connection.connect(instance);
                Collector<UpdateSimulationDistancePacket> collector = connection.trackIncoming(UpdateSimulationDistancePacket.class);

                env.process().eventHandler().call(new PlayerSpawnEvent(player, instance, true));

                collector.assertSingle();
                Assertions.assertEquals(SIMULATION_DISTANCE, collector.collect().getFirst().simulationDistance());
                Assertions.assertEquals(spawnPos, player.getPosition());
                Assertions.assertEquals(Material.STICK, player.getInventory().getItemStack(0).material(), "equip() must have applied the other registered item too");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Falling below the configured minimum height teleports the player back to spawn")
    @Test
    void fallingBelowMinHeightTeleportsToSpawn(Env env) {
        Instance instance = env.createFlatInstance();
        Pos spawnPos = new Pos(10, 100, 10);

        try (TestPlatform platform = TestPlatform.attach(env)) {
            SpawnModule module = new SpawnModule(instance, () -> spawnPos, platform.titan(), platform.itemRegistry());
            module.start();
            try {
                Player player = env.createPlayer(instance);
                Pos belowMin = new Pos(0, MIN_HEIGHT - 10, 0);
                player.teleport(belowMin);

                env.process().eventHandler().call(new PlayerMoveEvent(player, belowMin, true));

                Assertions.assertEquals(spawnPos, player.getPosition());
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Rising above the configured maximum height teleports the player back to spawn")
    @Test
    void risingAboveMaxHeightTeleportsToSpawn(Env env) {
        Instance instance = env.createFlatInstance();
        Pos spawnPos = new Pos(10, 100, 10);

        try (TestPlatform platform = TestPlatform.attach(env)) {
            SpawnModule module = new SpawnModule(instance, () -> spawnPos, platform.titan(), platform.itemRegistry());
            module.start();
            try {
                Player player = env.createPlayer(instance);
                Pos aboveMax = new Pos(0, MAX_HEIGHT + 10, 0);
                player.teleport(aboveMax);

                env.process().eventHandler().call(new PlayerMoveEvent(player, aboveMax, true));

                Assertions.assertEquals(spawnPos, player.getPosition());
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Staying within the configured height bounds does not teleport the player")
    @Test
    void withinHeightBoundsDoesNotTeleport(Env env) {
        Instance instance = env.createFlatInstance();
        Pos spawnPos = new Pos(10, 100, 10);

        try (TestPlatform platform = TestPlatform.attach(env)) {
            SpawnModule module = new SpawnModule(instance, () -> spawnPos, platform.titan(), platform.itemRegistry());
            module.start();
            try {
                Player player = env.createPlayer(instance);
                Pos withinBounds = new Pos(0, (MIN_HEIGHT + MAX_HEIGHT) / 2.0, 0);
                player.teleport(withinBounds);

                env.process().eventHandler().call(new PlayerMoveEvent(player, withinBounds, true));

                Assertions.assertEquals(withinBounds, player.getPosition());
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Once stopped, spawning no longer teleports, equips or sends the simulation distance")
    @Test
    void stopLeavesNoListenerBehind(Env env) {
        Instance instance = env.createFlatInstance();
        Pos spawnPos = new Pos(5, 64, 5);

        try (TestPlatform platform = TestPlatform.attach(env)) {
            SpawnModule module = new SpawnModule(instance, () -> spawnPos, platform.titan(), platform.itemRegistry());
            module.start();
            module.stop();

            TestConnection connection = env.createConnection();
            Player player = connection.connect(instance);
            Collector<UpdateSimulationDistancePacket> collector = connection.trackIncoming(UpdateSimulationDistancePacket.class);
            Pos before = player.getPosition();

            env.process().eventHandler().call(new PlayerSpawnEvent(player, instance, true));

            collector.assertEmpty();
            Assertions.assertEquals(before, player.getPosition(), "a stopped feature must not teleport the player to spawn any more");
        }
    }
}
