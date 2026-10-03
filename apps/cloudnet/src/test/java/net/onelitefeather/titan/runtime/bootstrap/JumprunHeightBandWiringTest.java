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

import io.avaje.config.Config;
import io.avaje.inject.BeanScope;
import java.util.List;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.client.play.ClientPlayerPositionPacket;
import net.minestom.server.network.packet.client.play.ClientTeleportConfirmPacket;
import net.minestom.server.network.packet.server.play.PlayerPositionAndLookPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.TestConnection;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.module.LobbyHeightBounds;
import net.onelitefeather.titan.core.module.item.LobbyItems;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.platform.luckperms.LuckPermsPermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * The spawn and jumprun columns together under one {@code spawn.minHeight}: a run that starts at
 * the lowest allowed block still ends in a reset to its start when the runner falls, and never in
 * the lobby spawn's out-of-bounds teleport. Both columns handle the same move event, spawn first.
 *
 * <p>Jumprun reads the bounds through a {@code Provider} of the spawn column's bean, so a run that
 * starts, or is refused, only because of {@code spawn.minHeight} proves the wiring too.
 */
@ExtendWith(MicrotusExtension.class)
@Timeout(30)
class JumprunHeightBandWiringTest {

    /** The flat instance is stone up to y 39, so the runner stands, and the run starts, at 40. */
    private static final double GROUND_Y = 40.0;

    /**
     * Fall threshold of 3 under the lowest block plus one move of 5, as the height band keeps
     * clear.
     */
    private static final int BAND_DEPTH = 8;

    private static final Pos LOBBY_SPAWN = new Pos(-40.5, GROUND_Y, 0.5);
    private static final Pos RUN_START = new Pos(0.5, GROUND_Y, 0.5);
    private static final int PRELOADED_CHUNK_RADIUS = 3;

    private String originalMinHeight;

    @BeforeEach
    void rememberMinHeight() {
        this.originalMinHeight = Config.get("spawn.minHeight");
    }

    @AfterEach
    void restoreMinHeight() {
        Config.setProperty("spawn.minHeight", this.originalMinHeight);
    }

    private static Instance loadedInstance(Env env) {
        Instance instance = env.createFlatInstance();
        for (int x = -PRELOADED_CHUNK_RADIUS; x <= PRELOADED_CHUNK_RADIUS; x++) {
            for (int z = -PRELOADED_CHUNK_RADIUS; z <= PRELOADED_CHUNK_RADIUS; z++) {
                instance.loadChunk(x, z).join();
            }
        }
        return instance;
    }

    private static BeanScope scope(Instance instance) {
        // Named mock, not the plain mock(Type) overload - see docs/lobby-modules.md,
        // "Permission-Plattform". The instance is bound so spawn does not send the player to
        // PlatformBeans' own, ungenerated one.
        return BeanScope.builder().forTesting().mock(FeatureFlags.class).mock(MapProvider.class, mapProvider -> Mockito.when(mapProvider.getActiveLobby()).thenReturn(new LobbyMap("test", LOBBY_SPAWN, List.of()))).mock(PermissionService.class, LuckPermsPermissionService.QUALIFIER).bean(InstanceContainer.class, (InstanceContainer) instance).bean(Instance.class, instance).build();
    }

    private static Player joinedAtRunStart(Env env, TestConnection connection, BeanScope scope, Instance instance) {
        Player player = connection.connect(instance, LOBBY_SPAWN);
        scope.get(LobbyItems.class).equip(player);
        player.teleport(RUN_START).join();
        player.refreshOnGround(true);
        return player;
    }

    private static void useJumprunItem(Env env, Player player) {
        ItemStack item = player.getInventory().getItemStack(0);
        env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, item, 0L));
    }

    /** Lets the block animation and any teleport finish. */
    private static void tick(Env env) {
        for (int tick = 0; tick < 20; tick++) {
            env.tick();
        }
    }

    private static boolean runShowsBlocks(Instance instance) {
        return instance.getEntities().stream().anyMatch(entity -> entity.getEntityType() == EntityType.BLOCK_DISPLAY);
    }

    /** The client reports a position in one move packet, as after a lag spike. */
    private static void clientMovesTo(Env env, Player player, Pos to) {
        env.process().packetListener().processClientPacket(new ClientTeleportConfirmPacket(player.getLastSentTeleportId()), player.getPlayerConnection());
        env.process().packetListener().processClientPacket(new ClientPlayerPositionPacket(to, false, false), player.getPlayerConnection());
    }

    @DisplayName("A fall from the lowest allowed block, in one move, resets the runner to the run start and not to the lobby spawn")
    @Test
    void fallingFromTheLowestAllowedBlockResetsToTheRunStart(Env env) {
        // The start block's top is GROUND_Y, the lowest block of any run here.
        Config.setProperty("spawn.minHeight", String.valueOf((int) GROUND_Y - BAND_DEPTH - 1));
        Instance instance = loadedInstance(env);
        BeanScope scope = scope(instance);
        try {
            TestConnection connection = env.createConnection();
            Player player = joinedAtRunStart(env, connection, scope, instance);
            Assertions.assertNotNull(scope.get(LobbyHeightBounds.class), "the spawn column must provide the bounds jumprun asks for");

            useJumprunItem(env, player);
            tick(env);
            Assertions.assertTrue(runShowsBlocks(instance), "a run must start at the lowest block the band allows");

            Collector<PlayerPositionAndLookPacket> teleports = connection.trackIncoming(PlayerPositionAndLookPacket.class);
            clientMovesTo(env, player, RUN_START.withY(GROUND_Y - BAND_DEPTH));
            tick(env);

            Assertions.assertFalse(runShowsBlocks(instance), "the fall must end the run and take its blocks down");
            // Minestom applies the reported position after the event, so the teleport packet is
            // what shows where the runner was sent.
            List<Point> sentTo = teleports.collect().stream().map(PlayerPositionAndLookPacket::position).toList();
            Assertions.assertEquals(List.of(RUN_START), sentTo, "the runner must be sent to the run start only, never to the lobby spawn");
        } finally {
            scope.close();
        }
    }

    @DisplayName("One block lower than the band allows, no run starts")
    @Test
    void aStartBelowTheBandIsRefused(Env env) {
        Config.setProperty("spawn.minHeight", String.valueOf((int) GROUND_Y - BAND_DEPTH));
        Instance instance = loadedInstance(env);
        BeanScope scope = scope(instance);
        try {
            Player player = joinedAtRunStart(env, env.createConnection(), scope, instance);

            useJumprunItem(env, player);
            tick(env);

            Assertions.assertFalse(runShowsBlocks(instance), "a start whose fall would reach spawn.minHeight must be refused");
        } finally {
            scope.close();
        }
    }
}
