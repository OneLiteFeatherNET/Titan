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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.ActionBarPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class JumprunMoveTest {

    private static final Pos STAND = StartedRun.STAND;

    private static long count(List<ServerPacket> packets, Class<? extends ServerPacket> type) {
        return packets.stream().filter(type::isInstance).count();
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void landingOnTheNextBlockShowsOneNewBlockAhead(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);

            List<ServerPacket> packets = run.landOnNext();

            assertEquals(1, count(packets, BlockChangePacket.class), "one block joins the window and none leaves it yet");
            assertTrue(fixture.module().isRunning(run.player()));
        }
    }

    @Test
    void landingOnTheBlockAfterTheNextShowsTwoNewBlocks(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            run.ahead().removeFirst();
            Collector<BlockChangePacket> sent = run.connection().trackIncoming(BlockChangePacket.class);

            fixture.landOn(run.player(), run.ahead().removeFirst());
            fixture.settle();

            assertEquals(2, sent.collect().size(), "both jumps count, so the window moves on by two");
        }
    }

    @Test
    void theStartBlockLeavingTheWindowIsNotTouched(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            run.landOnNext();
            run.landOnNext();

            List<ServerPacket> third = run.landOnNext();

            assertFalse(third.stream().anyMatch(packet -> packet instanceof BlockChangePacket change && change.blockPosition().sameBlock(new BlockVec(0, 39, 0))), "the real start block gets no packet when it leaves the window");
        }
    }

    @Test
    void theOldestFakeBlockIsTakenBackOnceItLeavesTheWindow(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Point oldest = run.ahead().peekFirst().blockPosition();
            run.landOnNext();
            run.landOnNext();
            run.landOnNext();

            List<ServerPacket> fourth = run.landOnNext();

            assertTrue(fourth.stream().anyMatch(packet -> packet instanceof BlockChangePacket change && change.blockPosition().sameBlock(oldest) && change.blockStateId() == Block.AIR.stateId()), "the real world is shown again where the oldest block was");
        }
    }

    @Test
    void theScoreAppearsInTheActionBarOnceTheAscentIsDone(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            List<ServerPacket> last = List.of();
            for (int landing = 0; landing <= JumprunFixture.ASCENT_JUMPS; landing++) {
                last = run.landOnNext();
            }

            ActionBarPacket bar = last.stream().filter(ActionBarPacket.class::isInstance).map(ActionBarPacket.class::cast).findFirst().orElseThrow();
            assertEquals(plain(fixture.messages().scoreActionBar(run.player().getLocale(), 1)), plain(bar.text()), "the first scored jump shows score 1");
        }
    }

    @Test
    void movingThroughTheAirOverABlockDoesNotAdvance(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            BlockChangePacket next = run.ahead().peekFirst();
            Collector<ServerPacket> sent = run.connection().trackIncoming();

            fixture.move(run.player(), new Pos(next.blockPosition().blockX() + 0.5, JumprunFixture.topOf(next), next.blockPosition().blockZ() + 0.5), false);

            assertTrue(sent.collect().isEmpty(), "only a landing counts");
        }
    }

    @Test
    void fallingMoreThanThreeBlocksBelowTheBlockEndsTheRunAndResetsTheBlocks(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Collector<BlockChangePacket> resets = run.connection().trackIncoming(BlockChangePacket.class);

            fixture.move(run.player(), STAND.withY(JumprunFixture.GROUND_Y - 3.5), false);

            assertFalse(fixture.module().isRunning(run.player()), "the fall ends the run");
            List<BlockChangePacket> packets = resets.collect();
            assertEquals(2, packets.size(), "both shown blocks are taken back");
            assertTrue(packets.stream().allMatch(packet -> packet.blockStateId() == Block.AIR.stateId()));
        }
    }

    @Test
    void aFallReportedByTheClientPacketEndsTheRunAndSetsThePlayerBackToTheStartPoint(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);

            run.player().teleport(STAND.add(6, 0, 0)).join();

            fixture.sendPositionPacket(run.player(), STAND.add(6, -3.5, 0), false);

            assertFalse(fixture.module().isRunning(run.player()), "the fall ends the run");
            assertEquals(STAND, run.player().getPosition(), "the player stands where the run began");
        }
    }

    @Test
    void aFallSetsThePlayerBackToTheStartPoint(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            run.player().teleport(STAND.add(6, 0, 0)).join();

            fixture.move(run.player(), STAND.add(6, -3.5, 0), false);

            assertEquals(STAND, run.player().getPosition(), "the player stands where the run began");
        }
    }

    @Test
    void aFallTellsThePlayerTheScore(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Collector<SystemChatPacket> chat = run.connection().trackIncoming(SystemChatPacket.class);

            fixture.move(run.player(), STAND.withY(JumprunFixture.GROUND_Y - 3.5), false);

            Component expected = fixture.messages().endScore(run.player().getLocale(), Mode.MEDIUM, 0);
            chat.assertSingle(packet -> assertEquals(expected, packet.message()));
        }
    }

    @Test
    void droppingExactlyThreeBlocksDoesNotEndTheRun(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);

            fixture.move(run.player(), STAND.withY(JumprunFixture.GROUND_Y - 3.0), false);

            assertTrue(fixture.module().isRunning(run.player()), "the run ends below three blocks, not at three");
        }
    }

    @Test
    void aRunEndsWithTheReachedScoreWhenNoFurtherBlockFits(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            for (int x = -40; x <= 40; x++) {
                for (int z = -40; z <= 40; z++) {
                    for (int y = 41; y <= 70; y++) {
                        run.instance().setBlock(x, y, z, Block.STONE);
                    }
                }
            }
            Collector<SystemChatPacket> chat = run.connection().trackIncoming(SystemChatPacket.class);

            while (fixture.module().isRunning(run.player())) {
                run.landOnNext();
            }

            Component expected = fixture.messages().endRecord(run.player().getLocale(), Mode.MEDIUM, 1);
            chat.assertSingle(packet -> assertEquals(expected, packet.message()));
        }
    }

    @Test
    void aMoveOfAPlayerWithoutARunChangesNothing(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Instance instance = JumprunFixture.loadedInstance(env);
            TestConnection connection = env.createConnection();
            Player player = connection.connect(instance, STAND);
            Collector<ServerPacket> sent = connection.trackIncoming();

            fixture.move(player, STAND.add(1, 0, 0), true);

            assertTrue(sent.collect().isEmpty());
        }
    }
}
