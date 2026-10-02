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
package net.onelitefeather.titan.feature.jumprun;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;

/** A started run of one player, with the blocks it was shown at the start. */
record StartedRun(JumprunFixture fixture, TestConnection connection, Player player,
                  Instance instance, Deque<BlockChangePacket> ahead) {

    static final Pos STAND = new Pos(0.5, JumprunFixture.GROUND_Y, 0.5);

    static StartedRun start(Env env, JumprunFixture fixture) {
        return start(env, fixture, JumprunFixture.loadedInstance(env), STAND);
    }

    static StartedRun start(Env env, JumprunFixture fixture, Instance instance, Pos stand) {
        return start(env, fixture, instance, stand, _ -> {
        });
    }

    /** Starts a run for a player whose best score was already stored before it began. */
    static StartedRun startWithBest(Env env, JumprunFixture fixture, Instance instance, int best) {
        return start(env, fixture, instance, STAND, player -> fixture.records().submit(player.getUuid(), best));
    }

    /** Starts the run and lets the first blocks land, which is when the player is sent them. */
    private static StartedRun start(Env env, JumprunFixture fixture, Instance instance, Pos stand, Consumer<Player> beforeStart) {
        return start(env, fixture, instance, stand, beforeStart, true);
    }

    /** Starts the run and returns at once, while the first blocks are still falling. */
    static StartedRun startFalling(Env env, JumprunFixture fixture, Instance instance, Pos stand) {
        return start(env, fixture, instance, stand, _ -> {
        }, false);
    }

    private static StartedRun start(Env env, JumprunFixture fixture, Instance instance, Pos stand, Consumer<Player> beforeStart, boolean settle) {
        TestConnection connection = env.createConnection();
        return start(fixture, instance, connection, connection.connect(instance, stand), beforeStart);
    }

    /**
     * Starts another run for a player who is already online, for example after a disconnect event.
     */
    static StartedRun startAgain(JumprunFixture fixture, Instance instance, TestConnection connection, Player player) {
        return start(fixture, instance, connection, player, _ -> {
        });
    }

    private static StartedRun start(JumprunFixture fixture, Instance instance, TestConnection connection, Player player, Consumer<Player> beforeStart) {
        beforeStart.accept(player);
        player.refreshOnGround(true);
        Collector<ServerPacket> shown = connection.trackIncoming();
        fixture.useItem(player);
        if (settle) {
            fixture.settle();
        }
        return new StartedRun(fixture, connection, player, instance, new ArrayDeque<>(courseBlocksInShownOrder(shown.collect())));
    }

    /** Reaches the next block ahead and stands still on it, as {@link #landOnNext()} otherwise. */
    List<ServerPacket> settleOnNext() {
        Collector<ServerPacket> sent = connection.trackIncoming();
        fixture.settleOn(player, ahead.removeFirst());
        fixture.settle();
        List<ServerPacket> packets = sent.collect();
        learn(packets);
        return packets;
    }

    /** Takes the blocks the player was sent meanwhile as the next ones ahead. */
    void learn(List<ServerPacket> packets) {
        ahead.addAll(courseBlocksInShownOrder(packets));
    }

    /** Lands on the next {@code count} blocks ahead, one after the other. */
    void landOnNext(int count) {
        for (int landing = 0; landing < count; landing++) {
            landOnNext();
        }
    }

    /** Lands on the next block ahead and returns everything the player was sent for it. */
    List<ServerPacket> landOnNext() {
        Collector<ServerPacket> sent = connection.trackIncoming();
        fixture.landOn(player, ahead.removeFirst());
        fixture.settle();
        List<ServerPacket> packets = sent.collect();
        learn(packets);
        return packets;
    }

    /**
     * The blocks land in no fixed order, but they were spawned nearest first, and the runner must
     * reach them in that order.
     */
    private static List<BlockChangePacket> courseBlocksInShownOrder(List<ServerPacket> packets) {
        List<BlockPos> spawned = packets.stream().filter(SpawnEntityPacket.class::isInstance).map(SpawnEntityPacket.class::cast).filter(spawn -> spawn.type() == EntityType.BLOCK_DISPLAY).map(spawn -> new BlockPos(spawn.position().blockX(), spawn.position().blockY(), spawn.position().blockZ())).toList();
        return packets.stream().filter(BlockChangePacket.class::isInstance).map(BlockChangePacket.class::cast).filter(JumprunFixture::isCourseBlock).sorted(Comparator.comparingInt(block -> spawned.indexOf(new BlockPos(block.blockPosition().blockX(), block.blockPosition().blockY(), block.blockPosition().blockZ())))).toList();
    }
}
