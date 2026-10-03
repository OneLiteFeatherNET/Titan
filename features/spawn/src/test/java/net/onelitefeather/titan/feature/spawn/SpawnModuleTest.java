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
package net.onelitefeather.titan.feature.spawn;

import io.avaje.config.Config;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.UpdateSimulationDistancePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.item.LobbyItems;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * {@code Env} (Cyano/Microtus) coverage for {@link SpawnModule}, built directly with a
 * {@link Mockito#mock(Class) mocked} {@link LobbyItems}: spawning instance and respawn point on
 * configuration, teleport plus simulation distance plus the {@code equip} delegation on spawn, and
 * the height-bounds teleport - plus that {@link SpawnModule#stop()} leaves no listener behind.
 *
 * <p>{@link LobbyItems} is mocked rather than its real implementation ({@code HotbarLobbyItems}):
 * that implementation lives in {@code features/hotbar}, a sibling column this column must not
 * depend on, even in tests. What actually placing an item in a player's inventory looks like is
 * {@code HotbarLobbyItems}'s own concern and its own test's job.
 */
@ExtendWith(MicrotusExtension.class)
class SpawnModuleTest {

    /**
     * Read from the facade rather than hardcoded, so a changed shipped default cannot silently
     * desync this test.
     */
    private static final int MIN_HEIGHT = Config.getAs(SpawnSettings.MIN_HEIGHT_KEY, Integer::parseInt);
    private static final int MAX_HEIGHT = Config.getAs(SpawnSettings.MAX_HEIGHT_KEY, Integer::parseInt);
    private static final int SIMULATION_DISTANCE = Config.getAs(SpawnSettings.SIMULATION_DISTANCE_KEY, Integer::parseInt);

    @DisplayName("The configuration event sets the spawning instance and the player's respawn point")
    @Test
    void configurationEventSetsSpawningInstanceAndRespawnPoint(Env env) throws InterruptedException {
        Instance targetInstance = env.createFlatInstance();
        Pos spawnPos = new Pos(1, 2, 3);
        LobbyItems lobbyItems = Mockito.mock(LobbyItems.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SpawnModule module = new SpawnModule(targetInstance, () -> spawnPos, new SpawnHeightBoundsFactory().lobbyHeightBounds(), titan.node(), lobbyItems);
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
        LobbyItems lobbyItems = Mockito.mock(LobbyItems.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SpawnModule module = new SpawnModule(instance, () -> spawnPos, new SpawnHeightBoundsFactory().lobbyHeightBounds(), titan.node(), lobbyItems);
            module.start();
            try {
                TestConnection connection = env.createConnection();
                Player player = connection.connect(instance);
                Collector<UpdateSimulationDistancePacket> collector = connection.trackIncoming(UpdateSimulationDistancePacket.class);

                env.process().eventHandler().call(new PlayerSpawnEvent(player, instance, true));

                collector.assertSingle();
                Assertions.assertEquals(SIMULATION_DISTANCE, collector.collect().getFirst().simulationDistance());
                Assertions.assertEquals(spawnPos, player.getPosition());
                // connection.connect() already triggers the player's own initial spawn, so equip()
                // may already have run once before the explicit call above.
                Mockito.verify(lobbyItems, Mockito.atLeastOnce()).equip(player);
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
        LobbyItems lobbyItems = Mockito.mock(LobbyItems.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SpawnModule module = new SpawnModule(instance, () -> spawnPos, new SpawnHeightBoundsFactory().lobbyHeightBounds(), titan.node(), lobbyItems);
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
        LobbyItems lobbyItems = Mockito.mock(LobbyItems.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SpawnModule module = new SpawnModule(instance, () -> spawnPos, new SpawnHeightBoundsFactory().lobbyHeightBounds(), titan.node(), lobbyItems);
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
        LobbyItems lobbyItems = Mockito.mock(LobbyItems.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SpawnModule module = new SpawnModule(instance, () -> spawnPos, new SpawnHeightBoundsFactory().lobbyHeightBounds(), titan.node(), lobbyItems);
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
        LobbyItems lobbyItems = Mockito.mock(LobbyItems.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SpawnModule module = new SpawnModule(instance, () -> spawnPos, new SpawnHeightBoundsFactory().lobbyHeightBounds(), titan.node(), lobbyItems);
            module.start();
            module.stop();

            TestConnection connection = env.createConnection();
            Player player = connection.connect(instance);
            Collector<UpdateSimulationDistancePacket> collector = connection.trackIncoming(UpdateSimulationDistancePacket.class);
            Pos before = player.getPosition();

            env.process().eventHandler().call(new PlayerSpawnEvent(player, instance, true));

            collector.assertEmpty();
            Assertions.assertEquals(before, player.getPosition(), "a stopped feature must not teleport the player to spawn any more");
            Mockito.verifyNoInteractions(lobbyItems);
        }
    }
}
