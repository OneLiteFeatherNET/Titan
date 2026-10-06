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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import net.onelitefeather.titan.feature.jumprun.course.TestBlocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class JumprunStartTest {

    private static final Pos STAND = new Pos(0.5, JumprunFixture.GROUND_Y, 0.5);

    private static Player standingPlayer(TestConnection connection, Instance instance, Pos at) {
        Player player = connection.connect(instance, at);
        player.refreshOnGround(true);
        return player;
    }

    @Test
    void startInTheOpenSendsTheNextTwoBlocksToThePlayerOnly(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        TestConnection connectionA = env.createConnection();
        Player a = standingPlayer(connectionA, instance, STAND);
        TestConnection connectionB = env.createConnection();
        standingPlayer(connectionB, instance, STAND);
        Collector<BlockChangePacket> forA = connectionA.trackIncoming(BlockChangePacket.class);
        Collector<BlockChangePacket> forB = connectionB.trackIncoming(BlockChangePacket.class);

        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            fixture.useItem(a);
            fixture.settle();

            assertTrue(fixture.module().isRunning(a), "a run begins");
            List<BlockChangePacket> shown = forA.collect();
            assertEquals(2, shown.size(), "the start block is real, so only the next two blocks are sent");
            forB.assertEmpty();
            for (BlockChangePacket packet : shown) {
                assertTrue(instance.getBlock(packet.blockPosition()).compare(Block.AIR), "the real world stays air at " + packet.blockPosition());
            }
        }
    }

    @Test
    void theStartBlockIsNeverSentAsAFakeBlock(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        TestConnection connection = env.createConnection();
        Player player = standingPlayer(connection, instance, STAND);
        Collector<BlockChangePacket> sent = connection.trackIncoming(BlockChangePacket.class);

        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            fixture.useItem(player);

            BlockVec underFeet = new BlockVec(0, 39, 0);
            assertFalse(sent.collect().stream().anyMatch(packet -> packet.blockPosition().sameBlock(underFeet)), "the block under the player's feet stays untouched");
        }
    }

    @Test
    void startInTheAirBeginsNoRunAndSaysThereIsNoRoom(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, STAND.withY(60));
        player.refreshOnGround(false);
        Collector<BlockChangePacket> blocks = connection.trackIncoming(BlockChangePacket.class);
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);

        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            fixture.useItem(player);

            assertFalse(fixture.module().isRunning(player), "no run in the air");
            blocks.assertEmpty();
            Component expected = fixture.messages().noSpace(player.getLocale());
            chat.assertSingle(packet -> assertEquals(expected, packet.message()));
        }
    }

    @Test
    void startUnderALowCeilingBeginsNoRunAndSaysThereIsNoRoom(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                instance.setBlock(x, 42, z, Block.STONE);
            }
        }
        TestConnection connection = env.createConnection();
        Player player = standingPlayer(connection, instance, STAND);
        Collector<BlockChangePacket> blocks = connection.trackIncoming(BlockChangePacket.class);
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);

        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            fixture.useItem(player);

            assertFalse(fixture.module().isRunning(player), "no run under a ceiling the ascent cannot pass");
            blocks.assertEmpty();
            Component expected = fixture.messages().noSpace(player.getLocale());
            chat.assertSingle(packet -> assertEquals(expected, packet.message()));
        }
    }

    @Test
    void startNearTheLobbyUpperLimitBeginsNoRunAndSaysThereIsNoRoom(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        TestConnection connection = env.createConnection();
        Player player = standingPlayer(connection, instance, STAND);
        Collector<BlockChangePacket> blocks = connection.trackIncoming(BlockChangePacket.class);
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);

        try (JumprunFixture fixture = JumprunFixture.start(env, TestBlocks.bounds(-64, 45))) {
            fixture.useItem(player);

            assertFalse(fixture.module().isRunning(player), "the ascent would climb past the lobby's upper limit");
            blocks.assertEmpty();
            Component expected = fixture.messages().noSpace(player.getLocale());
            chat.assertSingle(packet -> assertEquals(expected, packet.message()));
        }
    }

    @Test
    void usingTheItemAgainEndsTheRunAndRestoresTheRealBlocks(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        TestConnection connection = env.createConnection();
        Player player = standingPlayer(connection, instance, STAND);

        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Collector<BlockChangePacket> sent = connection.trackIncoming(BlockChangePacket.class);
            fixture.useItem(player);
            fixture.settle();
            List<BlockChangePacket> shown = sent.collect();

            Collector<BlockChangePacket> resets = connection.trackIncoming(BlockChangePacket.class);
            fixture.useItem(player);

            assertFalse(fixture.module().isRunning(player), "the second use aborts the run");
            List<BlockChangePacket> restored = resets.collect();
            assertEquals(Set.copyOf(shown.stream().map(BlockChangePacket::blockPosition).toList()), Set.copyOf(restored.stream().map(BlockChangePacket::blockPosition).toList()), "the shown blocks are reset");
            assertTrue(restored.stream().allMatch(packet -> packet.blockStateId() == Block.AIR.stateId()), "the real world is air there");
        }
    }

    @Test
    void usingTheItemAgainTellsThePlayerTheScoreWithoutCallingZeroARecord(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        TestConnection connection = env.createConnection();
        Player player = standingPlayer(connection, instance, STAND);

        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            fixture.useItem(player);
            Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);

            fixture.useItem(player);

            Component expected = fixture.messages().endScore(player.getLocale(), Mode.MEDIUM, 0);
            chat.assertSingle(packet -> assertEquals(expected, packet.message()));
        }
    }

    @Test
    void eachRunBeginsWhereThePlayerStands(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        TestConnection connection = env.createConnection();
        Player player = standingPlayer(connection, instance, STAND);
        Pos elsewhere = new Pos(20.5, JumprunFixture.GROUND_Y, 20.5);

        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Collector<BlockChangePacket> first = connection.trackIncoming(BlockChangePacket.class);
            fixture.useItem(player);
            List<BlockChangePacket> firstRun = first.collect();
            fixture.useItem(player);
            player.teleport(elsewhere).join();
            player.refreshOnGround(true);

            Collector<BlockChangePacket> second = connection.trackIncoming(BlockChangePacket.class);
            fixture.useItem(player);
            List<BlockChangePacket> secondRun = second.collect();

            assertTrue(isNear(firstRun, STAND), "the first run starts at the first spot");
            assertTrue(isNear(secondRun, elsewhere), "the second run starts at the second spot");
        }
    }

    /** Two jumps of at most gap 2 plus the block itself reach 7 blocks from the start in a line. */
    private static boolean isNear(List<BlockChangePacket> packets, Pos start) {
        return packets.stream().allMatch(packet -> Math.abs(packet.blockPosition().blockX() - start.x()) <= 7 && Math.abs(packet.blockPosition().blockZ() - start.z()) <= 7);
    }
}
