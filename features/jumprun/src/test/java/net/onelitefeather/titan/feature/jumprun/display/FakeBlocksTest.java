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
package net.onelitefeather.titan.feature.jumprun.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.feature.jumprun.course.CourseBlock;
import net.onelitefeather.titan.feature.jumprun.course.Surface;
import net.onelitefeather.titan.feature.jumprun.course.TestBlocks;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class FakeBlocksTest {

    private static final BlockPos AT = new BlockPos(0, 45, 0);
    private static final List<CourseBlock> ONE_STONE = List.of(TestBlocks.at(AT, Surface.FULL));

    private final FakeBlocks fakeBlocks = new FakeBlocks();

    @Test
    void showSendsTheBlockToTheTargetPlayerOnly(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection targetConnection = env.createConnection();
        Player target = targetConnection.connect(instance, new Pos(0, 40, 0));
        TestConnection otherConnection = env.createConnection();
        otherConnection.connect(instance, new Pos(0, 40, 0));
        Collector<BlockChangePacket> forTarget = targetConnection.trackIncoming(BlockChangePacket.class);
        Collector<BlockChangePacket> forOther = otherConnection.trackIncoming(BlockChangePacket.class);

        fakeBlocks.show(target, ONE_STONE);

        List<BlockChangePacket> sent = forTarget.collect();
        assertEquals(1, sent.size(), "the target gets one packet per block");
        assertEquals(new BlockVec(0, 45, 0), sent.getFirst().blockPosition(), "the packet names the block position");
        assertEquals(TestBlocks.shipped().of(Surface.FULL).blocks().getFirst().stateId(), sent.getFirst().blockStateId(), "the packet carries the surface block");
        forOther.assertEmpty();
    }

    @Test
    void showLeavesTheRealWorldUnchanged(Env env) {
        Instance instance = env.createFlatInstance();
        Player target = env.createPlayer(instance, new Pos(0, 40, 0));

        fakeBlocks.show(target, ONE_STONE);

        assertTrue(instance.getBlock(AT.x(), AT.y(), AT.z()).air(), "the instance still has air there");
    }

    @Test
    void resetSendsTheRealBlockFromTheInstance(Env env) {
        Instance instance = env.createFlatInstance();
        instance.setBlock(AT.x(), AT.y(), AT.z(), Block.DIAMOND_BLOCK);
        TestConnection connection = env.createConnection();
        Player target = connection.connect(instance, new Pos(0, 40, 0));
        Collector<BlockChangePacket> sent = connection.trackIncoming(BlockChangePacket.class);

        fakeBlocks.reset(target, ONE_STONE);

        List<BlockChangePacket> packets = sent.collect();
        assertEquals(1, packets.size());
        assertEquals(Block.DIAMOND_BLOCK.stateId(), packets.getFirst().blockStateId(), "the real block is restored");
    }

    @Test
    void resetOfAnAirPositionSendsAir(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player target = connection.connect(instance, new Pos(0, 40, 0));
        Collector<BlockChangePacket> sent = connection.trackIncoming(BlockChangePacket.class);

        fakeBlocks.reset(target, ONE_STONE);

        assertEquals(Block.AIR.stateId(), sent.collect().getFirst().blockStateId());
    }

    private static final List<CourseBlock> ONE_TOWER = List.of(TestBlocks.tower(AT, 3));

    @Test
    void showSendsOnePacketPerCellOfATower(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection targetConnection = env.createConnection();
        Player target = targetConnection.connect(instance, new Pos(0, 40, 0));
        Collector<BlockChangePacket> forTarget = targetConnection.trackIncoming(BlockChangePacket.class);

        fakeBlocks.show(target, ONE_TOWER);

        List<BlockChangePacket> sent = forTarget.collect();
        assertEquals(6, sent.size(), "the block plus its pillar and ladder cells, 2H cells in all");
        assertTrue(sent.stream().anyMatch(packet -> Block.fromStateId(packet.blockStateId()).id() == Block.LADDER.id()), "one packet carries the ladder");
    }

    @Test
    void resetSendsTheRealWorldForEveryCellOfATower(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection targetConnection = env.createConnection();
        Player target = targetConnection.connect(instance, new Pos(0, 40, 0));
        Collector<BlockChangePacket> afterReset = targetConnection.trackIncoming(BlockChangePacket.class);

        fakeBlocks.reset(target, ONE_TOWER);

        List<BlockChangePacket> sent = afterReset.collect();
        assertEquals(6, sent.size(), "every cell of the tower is sent back to the real world");
        for (BlockChangePacket packet : sent) {
            Point at = packet.blockPosition();
            assertEquals(instance.getBlock(at.blockX(), at.blockY(), at.blockZ()).stateId(), packet.blockStateId(), "the real block at " + at);
        }
    }
}
