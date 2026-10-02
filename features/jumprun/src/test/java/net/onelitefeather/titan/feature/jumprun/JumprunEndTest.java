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
import java.util.OptionalInt;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.Event;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class JumprunEndTest {

    /** Far enough from the course that a teleport back to the start would show. */
    private static final Pos ELSEWHERE = StartedRun.STAND.add(6, 0, 0);

    private static void call(Env env, Event event) {
        env.process().eventHandler().call(event);
    }

    @Test
    void glidingWithTheElytraEndsTheRunWithoutSettingThePlayerBack(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            run.player().teleport(ELSEWHERE).join();
            Collector<BlockChangePacket> resets = run.connection().trackIncoming(BlockChangePacket.class);

            call(env, new PlayerStartFlyingWithElytraEvent(run.player()));

            assertFalse(fixture.module().isRunning(run.player()), "gliding ends the run");
            assertEquals(ELSEWHERE, run.player().getPosition(), "the player is not moved");
            List<BlockChangePacket> packets = resets.collect();
            assertEquals(2, packets.size(), "both shown blocks are taken back");
            assertTrue(packets.stream().allMatch(packet -> packet.blockStateId() == Block.AIR.stateId()));
        }
    }

    @Test
    void dyingEndsTheRunAndTakesTheBlocksBack(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Collector<BlockChangePacket> resets = run.connection().trackIncoming(BlockChangePacket.class);

            call(env, new PlayerDeathEvent(run.player(), Component.empty(), Component.empty()));

            assertFalse(fixture.module().isRunning(run.player()), "death ends the run");
            assertEquals(2, resets.collect().size());
        }
    }

    @Test
    void disconnectingDropsTheRun(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);

            call(env, new PlayerDisconnectEvent(run.player()));

            assertFalse(fixture.module().isRunning(run.player()), "the lobby keeps no state of the run");
        }
    }

    @Test
    void disconnectingSendsNothingToThePlayer(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Collector<ServerPacket> sent = run.connection().trackIncoming();

            call(env, new PlayerDisconnectEvent(run.player()));

            assertTrue(sent.collect().isEmpty(), "a player who is leaving gets no messages and no resets");
        }
    }

    @Test
    void disconnectingKeepsTheScoreAsRecord(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            run.landOnNext(Phase.ASCENT_JUMPS + 1);

            call(env, new PlayerDisconnectEvent(run.player()));

            assertEquals(OptionalInt.of(1), fixture.records().best(run.player().getUuid()), "the record survives the disconnect");
        }
    }

    @Test
    void aScoreAboveTheRecordIsReportedAsNewRecord(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            run.landOnNext(Phase.ASCENT_JUMPS + 1);
            Collector<SystemChatPacket> chat = run.connection().trackIncoming(SystemChatPacket.class);

            fixture.useItem(run.player());

            Component expected = fixture.messages().endRecord(run.player().getLocale(), 1);
            chat.assertSingle(packet -> assertEquals(expected, packet.message()));
            assertEquals(OptionalInt.of(1), fixture.records().best(run.player().getUuid()));
        }
    }

    @Test
    void aScoreBelowTheRecordIsReportedWithoutRecordAndKeepsTheRecord(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            fixture.records().submit(run.player().getUuid(), 5);
            run.landOnNext(Phase.ASCENT_JUMPS + 1);
            Collector<SystemChatPacket> chat = run.connection().trackIncoming(SystemChatPacket.class);

            fixture.useItem(run.player());

            Component expected = fixture.messages().endScore(run.player().getLocale(), 1);
            chat.assertSingle(packet -> assertEquals(expected, packet.message()));
            assertEquals(OptionalInt.of(5), fixture.records().best(run.player().getUuid()));
        }
    }
}
