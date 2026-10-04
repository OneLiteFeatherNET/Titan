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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Predicate;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** What the real module reports for the starts, ends and leaderboard refreshes it handles. */
@ExtendWith(MicrotusExtension.class)
class JumprunTelemetryTest {

    private static final AttributeKey<String> END_REASON = AttributeKey.stringKey("jumprun.end.reason");
    private static final AttributeKey<String> OUTCOME = AttributeKey.stringKey("jumprun.start.outcome");

    private final TestTelemetry testTelemetry = TestTelemetry.create();
    private final Telemetry telemetry = testTelemetry.telemetry();
    private final Queue<Runnable> pending = new ArrayDeque<>();

    @AfterEach
    void close() {
        testTelemetry.close();
    }

    private JumprunFixture start(Env env, RunRecords records) {
        return JumprunFixture.start(env, records, Optional.empty(), Runnable::run, telemetry);
    }

    /**
     * A task the way the database writer queues one: it keeps the context of whoever handed it
     * over.
     */
    private void childSpanLater(String name) {
        pending.add(Context.current().wrap(() -> telemetry.tracer().spanBuilder(name).startSpan().end()));
    }

    private void runPending() {
        while (!pending.isEmpty()) {
            pending.remove().run();
        }
    }

    private static RunRecords recordsThat(Predicate<FinishedRun> onSubmit) {
        return new RunRecords() {
            @Override
            public OptionalInt best(UUID player, Mode mode) {
                return OptionalInt.empty();
            }

            @Override
            public boolean submit(FinishedRun run) {
                return onSubmit.test(run);
            }

            @Override
            public void load(UUID player) {
            }

            @Override
            public void forget(UUID player) {
            }
        };
    }

    @Test
    void aFallInTheRealModuleEndsInOneSpanWithTheFallDetails(Env env) {
        try (JumprunFixture fixture = start(env, new InMemoryRunRecords())) {
            StartedRun run = StartedRun.start(env, fixture);
            run.settleOnNext();

            fixture.sendPositionPacket(run.player(), run.player().getPosition().add(0, -5, 0), false);

            SpanData span = testTelemetry.span("jumprun.end");
            assertEquals("FALL", testTelemetry.attribute(span, END_REASON), "reason");
            assertEquals(run.player().getUuid().toString(), testTelemetry.attribute(span, Telemetry.USER_ID), "player uuid");
            assertEquals(StatusCode.UNSET, span.getStatus().getStatusCode(), "a fall is no error");
            assertNotNull(testTelemetry.attribute(span, AttributeKey.doubleKey("jumprun.fall.threshold")), "the threshold is there");
        }
    }

    @Test
    void aSecondEndOfTheSameRunMakesNoSecondSpan(Env env) {
        try (JumprunFixture fixture = start(env, new InMemoryRunRecords())) {
            StartedRun run = StartedRun.start(env, fixture);

            env.process().eventHandler().call(new PlayerDisconnectEvent(run.player()));
            fixture.stopModule();

            assertEquals("DISCONNECT", testTelemetry.attribute(testTelemetry.span("jumprun.end"), END_REASON), "only the disconnect ended the run");
        }
    }

    @Test
    void aFailingSubmitMarksTheEndSpanAndReachesTheCaller(Env env) {
        IllegalStateException failure = new IllegalStateException("database is gone");
        try (JumprunFixture fixture = start(env, recordsThat(run -> {
            throw failure;
        }))) {
            StartedRun run = StartedRun.start(env, fixture);

            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> fixture.module().toggle(run.player()));

            assertSame(failure, thrown, "the caller sees the failure");
            assertEquals(StatusCode.ERROR, testTelemetry.span("jumprun.end").getStatus().getStatusCode(), "the span is marked");
        }
    }

    @Test
    void aWriteHandedOverByTheEndRunsUnderTheEndSpan(Env env) {
        try (JumprunFixture fixture = start(env, recordsThat(run -> {
            childSpanLater("db.write");
            return false;
        }))) {
            StartedRun run = StartedRun.start(env, fixture);

            fixture.module().toggle(run.player());
            runPending();

            assertEquals(testTelemetry.span("jumprun.end").getSpanId(), testTelemetry.span("db.write").getParentSpanId(), "the write is a child of the end");
        }
    }

    @Test
    void startingWithoutRoomMakesAStartSpanAndNoEndSpan(Env env) {
        try (JumprunFixture fixture = start(env, new InMemoryRunRecords())) {
            Player player = env.createConnection().connect(JumprunFixture.loadedInstance(env), StartedRun.STAND);
            player.refreshOnGround(false);

            fixture.useItem(player);

            assertEquals("no_room", testTelemetry.attribute(testTelemetry.span("jumprun.start"), OUTCOME), "outcome");
            assertTrue(testTelemetry.spans().stream().noneMatch(span -> span.getName().equals("jumprun.end")), "nothing ended");
            assertFalse(fixture.module().isRunning(player), "no run began");
        }
    }

    @Test
    void aStartThatFindsRoomIsMarkedStarted(Env env) {
        try (JumprunFixture fixture = start(env, new InMemoryRunRecords())) {
            StartedRun run = StartedRun.start(env, fixture);

            assertEquals("started", testTelemetry.attribute(testTelemetry.span("jumprun.start"), OUTCOME), "outcome");
            assertEquals(1, testTelemetry.counter("titan.jumprun.runs.started", Attributes.of(AttributeKey.stringKey("mode"), "MEDIUM", AttributeKey.stringKey("outcome"), "started")), "counter");
            assertTrue(fixture.module().isRunning(run.player()), "the run is on");
        }
    }

    @Test
    void aLeaderboardRefreshSpanIsMadeInTheExecutorTaskWithItsChildrenUnderIt(Env env) {
        RunStore store = new RunStore() {
            @Override
            public Map<Mode, Integer> bestsOf(UUID player) {
                return Map.of();
            }

            @Override
            public Map<Mode, TopThree> topThreeOfEveryMode() {
                telemetry.tracer().spanBuilder("db.read").startSpan().end();
                return Map.of();
            }

            @Override
            public void append(FinishedRun run) {
            }
        };
        try (JumprunFixture fixture = JumprunFixture.start(env, new InMemoryRunRecords(), Optional.of(new Leaderboard(store)), pending::add, telemetry)) {
            fixture.module().refreshLeaderboard();
            assertTrue(testTelemetry.spans().isEmpty(), "handing the refresh over makes no span");

            runPending();

            assertEquals(testTelemetry.span("jumprun.leaderboard.refresh").getSpanId(), testTelemetry.span("db.read").getParentSpanId(), "the read is a child of the refresh");
        }
    }

    @Test
    void landingsMakeNoSpan(Env env) {
        try (JumprunFixture fixture = start(env, new InMemoryRunRecords())) {
            StartedRun run = StartedRun.start(env, fixture);
            int before = testTelemetry.spans().size();

            run.settleOnNext();
            run.settleOnNext();

            assertEquals(before, testTelemetry.spans().size(), "landings and ticks produce no span");
        }
    }
}
