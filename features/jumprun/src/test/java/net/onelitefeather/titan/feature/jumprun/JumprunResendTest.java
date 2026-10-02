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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.player.PlayerChunkLoadEvent;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.network.packet.client.play.ClientPlayerActionPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.ChunkDataPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class JumprunResendTest {

    private static void call(Env env, Event event) {
        env.process().eventHandler().call(event);
    }

    private static List<Point> positions(List<BlockChangePacket> packets) {
        return packets.stream().map(BlockChangePacket::blockPosition).toList();
    }

    /**
     * The course leads where its seed and heading take it, so the chunk is taken from the window.
     */
    private static int chunkX(StartedRun run) {
        return run.ahead().peekFirst().blockPosition().blockX() >> 4;
    }

    private static int chunkZ(StartedRun run) {
        return run.ahead().peekFirst().blockPosition().blockZ() >> 4;
    }

    private static List<BlockChangePacket> inChunk(StartedRun run, int chunkX, int chunkZ) {
        return run.ahead().stream().filter(packet -> packet.blockPosition().blockX() >> 4 == chunkX && packet.blockPosition().blockZ() >> 4 == chunkZ).toList();
    }

    /**
     * Cyano's test player sends a chunk at once and fires no event, so this replays what Minestom's
     * own {@code sendPendingChunks} does for a real player: the chunk packet, then the event.
     */
    @Test
    void theBlocksFollowTheChunkPacketWhenTheChunkIsReportedLoaded(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Collector<ServerPacket> sent = run.connection().trackIncoming();

            int chunkX = chunkX(run);
            int chunkZ = chunkZ(run);
            run.player().sendChunk(run.instance().getChunk(chunkX, chunkZ));
            call(env, new PlayerChunkLoadEvent(run.player(), chunkX, chunkZ));

            List<ServerPacket> packets = sent.collect();
            int chunkAt = indexOfFirst(packets, ChunkDataPacket.class);
            assertTrue(chunkAt >= 0, "the chunk was sent to the player");
            List<ServerPacket> afterChunk = packets.subList(chunkAt + 1, packets.size());
            long blocksAfterChunk = afterChunk.stream().filter(BlockChangePacket.class::isInstance).count();
            assertEquals(inChunk(run, chunkX, chunkZ).size(), blocksAfterChunk, "every visible block of the chunk follows it, so the chunk cannot paint over it");
        }
    }

    @Test
    void theChunkLoadEventSendsTheBlocksInThatChunkAgain(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            int chunkX = chunkX(run);
            int chunkZ = chunkZ(run);
            List<Point> visible = positions(inChunk(run, chunkX, chunkZ));
            Collector<BlockChangePacket> sent = run.connection().trackIncoming(BlockChangePacket.class);

            call(env, new PlayerChunkLoadEvent(run.player(), chunkX, chunkZ));

            assertEquals(visible, positions(sent.collect()), "the blocks of the window that lie in the loaded chunk");
        }
    }

    @Test
    void aChunkWithoutBlocksOfTheRunSendsNothing(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Collector<BlockChangePacket> sent = run.connection().trackIncoming(BlockChangePacket.class);

            call(env, new PlayerChunkLoadEvent(run.player(), 3, 3));

            assertTrue(sent.collect().isEmpty());
        }
    }

    @Test
    void usingARealBlockNextToTheRunSendsNothing(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Collector<BlockChangePacket> sent = run.connection().trackIncoming(BlockChangePacket.class);

            run.fixture().sendClientPacket(run.player(), new ClientPlayerBlockPlacementPacket(PlayerHand.MAIN, new BlockVec(0, 39, 0), BlockFace.TOP, 0.5f, 1f, 0.5f, false, false, 1));
            env.tick();

            assertTrue(sent.collect().isEmpty(), "the start block is real and not part of the fake window");
        }
    }

    @Test
    void aRunThatEndedBeforeTheNextTickIsNotSentAgain(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Point block = run.ahead().peekFirst().blockPosition();
            run.fixture().sendClientPacket(run.player(), new ClientPlayerBlockPlacementPacket(PlayerHand.MAIN, block, BlockFace.TOP, 0.5f, 1f, 0.5f, false, false, 1));
            fixture.module().toggle(run.player());
            Collector<BlockChangePacket> sent = run.connection().trackIncoming(BlockChangePacket.class);

            env.tick();

            assertTrue(sent.collect().isEmpty(), "no fake block comes back after the run is over");
        }
    }

    @Test
    void severalPacketsInOneTickSendTheWindowOnce(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Point block = run.ahead().peekFirst().blockPosition();
            fixture.sendClientPacket(run.player(), new ClientPlayerActionPacket(ClientPlayerActionPacket.Status.STARTED_DIGGING, block, BlockFace.TOP, 1));
            fixture.sendClientPacket(run.player(), new ClientPlayerActionPacket(ClientPlayerActionPacket.Status.FINISHED_DIGGING, block, BlockFace.TOP, 2));
            Collector<BlockChangePacket> sent = run.connection().trackIncoming(BlockChangePacket.class);

            env.tick();

            long courseBlocks = sent.collect().stream().filter(JumprunFixture::isCourseBlock).count();
            assertEquals(run.ahead().size(), courseBlocks, "one batch of the whole window, not one per packet");
        }
    }

    private static int indexOfFirst(List<ServerPacket> packets, Class<? extends ServerPacket> type) {
        for (int index = 0; index < packets.size(); index++) {
            if (type.isInstance(packets.get(index))) {
                return index;
            }
        }
        return -1;
    }
}
