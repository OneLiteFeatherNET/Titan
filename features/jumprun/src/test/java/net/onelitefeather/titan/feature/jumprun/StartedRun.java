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
import java.util.Deque;
import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;

/** A started run of one player, with the blocks it was shown at the start. */
record StartedRun(JumprunFixture fixture, TestConnection connection, Player player,
                  Instance instance, Deque<BlockChangePacket> ahead) {

    static final Pos STAND = new Pos(0.5, JumprunFixture.GROUND_Y, 0.5);

    static StartedRun start(Env env, JumprunFixture fixture) {
        Instance instance = JumprunFixture.loadedInstance(env);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, STAND);
        player.refreshOnGround(true);
        Collector<BlockChangePacket> shown = connection.trackIncoming(BlockChangePacket.class);
        fixture.useItem(player);
        return new StartedRun(fixture, connection, player, instance, new ArrayDeque<>(shown.collect()));
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
        List<ServerPacket> packets = sent.collect();
        packets.stream().filter(BlockChangePacket.class::isInstance).map(BlockChangePacket.class::cast).filter(packet -> packet.blockStateId() != Block.AIR.stateId()).forEach(ahead::addLast);
        return packets;
    }
}

