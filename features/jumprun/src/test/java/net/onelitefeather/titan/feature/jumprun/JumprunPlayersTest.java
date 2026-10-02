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

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** What one player's run does, and does not do, to the others in the lobby. */
@ExtendWith(MicrotusExtension.class)
class JumprunPlayersTest {

    private static final Pos OTHER_STAND = StartedRun.STAND.add(0, 0, 8);

    /** Counts the submitted scores, so a run that is submitted twice shows. */
    private static final class CountingRecords implements RunRecords {

        private final InMemoryRunRecords delegate = new InMemoryRunRecords();
        private final List<Integer> submitted = new ArrayList<>();

        @Override
        public OptionalInt best(UUID player) {
            return delegate.best(player);
        }

        @Override
        public boolean submit(UUID player, int score) {
            submitted.add(score);
            return delegate.submit(player, score);
        }
    }

    @Test
    void aBystanderIsSentNoneOfAnotherPlayersBlocks(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Instance instance = JumprunFixture.loadedInstance(env);
            var bystander = env.createConnection();
            bystander.connect(instance, OTHER_STAND);
            Collector<BlockChangePacket> seenByBystander = bystander.trackIncoming(BlockChangePacket.class);

            StartedRun runner = StartedRun.start(env, fixture, instance, StartedRun.STAND);
            runner.landOnNext(2);

            assertTrue(seenByBystander.collect().isEmpty(), "fake blocks go to the player who runs only");
        }
    }

    @Test
    void endingOnePlayersRunLeavesTheOthersBlocksAndRunAlone(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Instance instance = JumprunFixture.loadedInstance(env);
            StartedRun first = StartedRun.start(env, fixture, instance, StartedRun.STAND);
            StartedRun second = StartedRun.start(env, fixture, instance, OTHER_STAND);
            Collector<BlockChangePacket> seenBySecond = second.connection().trackIncoming(BlockChangePacket.class);

            fixture.useItem(first.player());

            assertFalse(fixture.module().isRunning(first.player()), "the first run is over");
            assertTrue(seenBySecond.collect().isEmpty(), "the second player's blocks are not reset");
            assertTrue(fixture.module().isRunning(second.player()), "the second run goes on");
        }
    }

    @Test
    void theOtherPlayersRunKeepsShowingBlocksAfterOneEnds(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Instance instance = JumprunFixture.loadedInstance(env);
            StartedRun first = StartedRun.start(env, fixture, instance, StartedRun.STAND);
            StartedRun second = StartedRun.start(env, fixture, instance, OTHER_STAND);
            fixture.useItem(first.player());

            List<ServerPacket> sent = second.landOnNext();

            assertTrue(sent.stream().anyMatch(BlockChangePacket.class::isInstance), "the second run still shows its next block");
        }
    }

    @Test
    void endingARunTwiceSubmitsTheScoreAndTellsThePlayerOnce(Env env) {
        CountingRecords records = new CountingRecords();
        try (JumprunFixture fixture = JumprunFixture.start(env, records)) {
            StartedRun run = StartedRun.start(env, fixture);
            run.landOnNext(JumprunFixture.ASCENT_JUMPS + 1);
            Collector<SystemChatPacket> chat = run.connection().trackIncoming(SystemChatPacket.class);

            fixture.useItem(run.player());
            env.process().eventHandler().call(new PlayerDisconnectEvent(run.player()));

            assertEquals(List.of(1), records.submitted, "the score is submitted once");
            assertEquals(1, chat.collect().size(), "the player gets one end message");
        }
    }
}
