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
package net.onelitefeather.titan.runtime.bootstrap;

import io.avaje.inject.BeanScope;
import java.util.List;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.module.SpawnReturn;
import net.onelitefeather.titan.core.module.item.LobbyItems;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.feature.navigator.NavigatorModule;
import net.onelitefeather.titan.platform.luckperms.LuckPermsPermissionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * Cross-column acceptance for returning to the spawn: with every column wired through the real
 * {@link BeanScope}, {@code /spawn} and the navigator's spawn entry (slot 2) each end a running
 * Jump &amp; Run and put the player at the lobby spawn.
 *
 * <p>The scope is bound to {@code env}'s own instance and command manager, so nothing global is
 * touched and every test builds its own scope.
 */
@ExtendWith(MicrotusExtension.class)
@Timeout(30)
class SpawnReturnWiringTest {

    private static final Pos SPAWN = new Pos(-40.5, 40, 0.5);
    private static final Pos START = new Pos(0.5, 40, 0.5);
    private static final int PRELOADED_CHUNK_RADIUS = 3;
    private static final int SETTLE_TICKS = 20;

    private record Scene(BeanScope scope, Instance instance, Player player,
                         Collector<SystemChatPacket> chat,
                         ItemStack feather) implements AutoCloseable {

        static Scene start(Env env) {
            Instance instance = env.createFlatInstance();
            for (int x = -PRELOADED_CHUNK_RADIUS; x <= PRELOADED_CHUNK_RADIUS; x++) {
                for (int z = -PRELOADED_CHUNK_RADIUS; z <= PRELOADED_CHUNK_RADIUS; z++) {
                    instance.loadChunk(x, z).join();
                }
            }
            BeanScope scope = BeanScope.builder().forTesting().mock(FeatureFlags.class).mock(MapProvider.class, mapProvider -> Mockito.when(mapProvider.getActiveLobby()).thenReturn(new LobbyMap("test", SPAWN, List.of()))).mock(PermissionService.class, LuckPermsPermissionService.QUALIFIER).bean(InstanceContainer.class, (InstanceContainer) instance).bean(Instance.class, instance).bean(CommandManager.class, env.process().command()).build();
            TestConnection connection = env.createConnection();
            Player player = connection.connect(instance, START);
            player.teleport(START).join();
            player.refreshOnGround(true);
            scope.get(LobbyItems.class).equip(player);
            ItemStack feather = player.getInventory().getItemStack(4);
            Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);
            return new Scene(scope, instance, player, chat, feather);
        }

        void startRun(Env env) {
            ItemStack runItem = this.player.getInventory().getItemStack(0);
            env.process().eventHandler().call(new PlayerUseItemEvent(this.player, PlayerHand.MAIN, runItem, 0L));
            settle(env);
        }

        int runBlocks() {
            return (int) this.instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.BLOCK_DISPLAY).count();
        }

        @Override
        public void close() {
            this.scope.close();
        }
    }

    private static void settle(Env env) {
        for (int tick = 0; tick < SETTLE_TICKS; tick++) {
            env.tick();
        }
    }

    @DisplayName("With every column wired, SpawnReturn resolves, the navigator started and /spawn is registered")
    @Test
    void wiringResolvesSpawnReturnStartsTheNavigatorAndRegistersTheCommand(Env env) {
        try (Scene scene = Scene.start(env)) {
            Assertions.assertNotNull(scene.scope().get(SpawnReturn.class), "SpawnReturn must be a bean of the spawn column");
            Assertions.assertNotNull(scene.scope().get(NavigatorModule.class), "the navigator must have started, which resolves its SpawnReturn provider");
            Assertions.assertTrue(env.process().command().commandExists("spawn"), "/spawn must be registered with the command manager");
        }
    }

    @DisplayName("/spawn during a Jump & Run run ends the run and puts the player at the lobby spawn")
    @Test
    void spawnCommandEndsTheRunAndReturnsThePlayer(Env env) {
        try (Scene scene = Scene.start(env)) {
            scene.startRun(env);
            Assertions.assertTrue(scene.runBlocks() > 0, "precondition: the run must have started and shown its blocks");

            env.process().command().execute(scene.player(), "spawn");
            List<SystemChatPacket> messages = scene.chat().collect();
            settle(env);

            Assertions.assertEquals(0, scene.runBlocks(), "no run block may remain after /spawn");
            Assertions.assertEquals(SPAWN, scene.player().getPosition(), "the player must stand at the lobby spawn");
            Assertions.assertEquals(2, messages.size(), "the run's end message and the spawn confirmation must arrive");
        }
    }

    @DisplayName("Clicking the navigator's spawn entry (slot 2) during a run ends it and puts the player at the lobby spawn")
    @Test
    void navigatorSlotTwoEndsTheRunAndReturnsThePlayer(Env env) {
        try (Scene scene = Scene.start(env)) {
            scene.startRun(env);
            Assertions.assertTrue(scene.runBlocks() > 0, "precondition: the run must have started and shown its blocks");
            env.process().eventHandler().call(new PlayerUseItemEvent(scene.player(), PlayerHand.MAIN, scene.feather(), 0L));
            AbstractInventory navigator = scene.player().getOpenInventory();
            Assertions.assertNotNull(navigator, "the feather must open the navigator");
            Assertions.assertEquals(Material.COMPASS, navigator.getItemStack(2).material(), "navigator slot 2 must be the spawn entry");

            env.process().eventHandler().call(new InventoryPreClickEvent(navigator, scene.player(), new Click.Left(2)));
            settle(env);

            Assertions.assertEquals(0, scene.runBlocks(), "no run block may remain after the click");
            Assertions.assertEquals(SPAWN, scene.player().getPosition(), "the player must stand at the lobby spawn");
        }
    }
}
