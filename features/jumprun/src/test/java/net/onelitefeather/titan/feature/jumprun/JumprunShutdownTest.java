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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import net.kyori.adventure.translation.GlobalTranslator;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.ActionBarPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.testfixtures.EventListenerCounter;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class JumprunShutdownTest {

    @Test
    void stoppingDetachesTheColumnsListeners(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            JumprunModule module = new JumprunModule(titan.node(), () -> null, List::of, new InMemoryRunRecords(), new RunMessages(), () -> JumprunFixture.SEED);
            module.start();
            EventNode<Event> featureNode = titan.node().getChildren().iterator().next();
            assertTrue(EventListenerCounter.countListeners(featureNode) > 0, "the column listens while it runs");

            module.stop();

            assertTrue(titan.node().getChildren().isEmpty(), "the column's node is gone from the titan node");
            assertEquals(0, EventListenerCounter.countListeners(titan.node()), "nothing is left on the titan node itself");
        }
    }

    @Test
    void stoppingEndsRunningRunsAndRestoresTheirBlocks(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Collector<BlockChangePacket> resets = run.connection().trackIncoming(BlockChangePacket.class);

            fixture.stopModule();

            assertFalse(fixture.module().isRunning(run.player()), "no run survives the stop");
            List<BlockChangePacket> packets = resets.collect();
            assertEquals(2, packets.size(), "both shown blocks are taken back");
            assertTrue(packets.stream().allMatch(packet -> packet.blockStateId() == Block.AIR.stateId()));
        }
    }

    @Test
    void stoppingTellsNoPlayerAnything(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Collector<ServerPacket> sent = run.connection().trackIncoming();

            fixture.stopModule();

            assertTrue(sent.collect().stream().noneMatch(packet -> packet instanceof SystemChatPacket || packet instanceof ActionBarPacket), "the lobby is shutting down, so there is no score to report");
        }
    }

    @Test
    void stoppingDoesNotCountAnUnfinishedRunAsRecord(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);

            fixture.stopModule();

            assertEquals(OptionalInt.empty(), fixture.records().best(run.player().getUuid()));
        }
    }

    @Test
    void stoppingRemovesTheTranslationStore(Env env) {
        String key = RunMessages.KEYS.getFirst();
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            assertTrue(GlobalTranslator.translator().canTranslate(key, Locale.ENGLISH), "the column's store translates its keys while it runs");

            fixture.stopModule();

            assertFalse(GlobalTranslator.translator().canTranslate(key, Locale.ENGLISH), "the column's store is gone again");
        }
    }

    @Test
    void stoppingAColumnThatNeverStartedDoesNotFail(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            JumprunModule module = new JumprunModule(titan.node(), () -> null, List::of, new InMemoryRunRecords(), new RunMessages(), () -> JumprunFixture.SEED);

            assertDoesNotThrow(module::stop);
        }
    }
}
