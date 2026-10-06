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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.Optional;
import java.util.UUID;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import net.onelitefeather.titan.feature.jumprun.persistence.EndReason;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RunTelemetryTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-00000000a1ec");
    private static final String PLAYER_NAME = "Alex";
    private static final RunTelemetry.Fall FALL = new RunTelemetry.Fall(31.5, 33.0, 7);

    private final TestTelemetry testTelemetry = TestTelemetry.create();
    private final RunTelemetry telemetry = new RunTelemetry(testTelemetry.telemetry());

    @AfterEach
    void close() {
        testTelemetry.close();
    }

    private static RunTelemetry.Ending ending(EndReason reason, Optional<RunTelemetry.Fall> fall) {
        return new RunTelemetry.Ending(PLAYER, Mode.MEDIUM, reason, 12, fall);
    }

    private static Attributes ended(EndReason reason, Mode mode) {
        return Attributes.of(AttributeKey.stringKey("reason"), reason.name(), AttributeKey.stringKey("mode"), mode.name());
    }

    private static Attributes started(Mode mode, String outcome) {
        return Attributes.of(AttributeKey.stringKey("mode"), mode.name(), AttributeKey.stringKey("outcome"), outcome);
    }

    @Test
    void aFallEndsInASpanWithItsOutcomeAndTheFallDetails() {
        telemetry.end(ending(EndReason.FALL, Optional.of(FALL)), () -> false);

        SpanData span = testTelemetry.span("jumprun.end");
        assertEquals("FALL", testTelemetry.attribute(span, AttributeKey.stringKey("jumprun.end.reason")), "reason");
        assertEquals("MEDIUM", testTelemetry.attribute(span, AttributeKey.stringKey("jumprun.mode")), "mode");
        assertEquals(12L, testTelemetry.attribute(span, AttributeKey.longKey("jumprun.score")), "score");
        assertEquals(PLAYER.toString(), testTelemetry.attribute(span, Telemetry.USER_ID), "the player's uuid");
        assertEquals(31.5, testTelemetry.attribute(span, AttributeKey.doubleKey("jumprun.fall.y")), "fall height");
        assertEquals(33.0, testTelemetry.attribute(span, AttributeKey.doubleKey("jumprun.fall.threshold")), "fall threshold");
        assertEquals(7L, testTelemetry.attribute(span, AttributeKey.longKey("jumprun.fall.course_index")), "last block");
        assertEquals(StatusCode.UNSET, span.getStatus().getStatusCode(), "a fall is no error");
    }

    @Test
    void anAbortCarriesNoFallDetails() {
        telemetry.end(ending(EndReason.ABORT, Optional.empty()), () -> false);

        SpanData span = testTelemetry.span("jumprun.end");
        assertEquals("ABORT", testTelemetry.attribute(span, AttributeKey.stringKey("jumprun.end.reason")));
        assertTrue(span.getAttributes().asMap().keySet().stream().noneMatch(key -> key.getKey().startsWith("jumprun.fall.")), "no fall.* attribute");
    }

    @Test
    void theSpanReportsWhetherTheRunWasARecord() {
        boolean record = telemetry.end(ending(EndReason.FALL, Optional.of(FALL)), () -> true);

        assertTrue(record, "the submit's answer is handed back");
        assertEquals(Boolean.TRUE, testTelemetry.attribute(testTelemetry.span("jumprun.end"), AttributeKey.booleanKey("jumprun.record")));
    }

    @Test
    void aFailingSubmitMarksTheSpanAndReachesTheCaller() {
        IllegalStateException failure = new IllegalStateException("database is gone");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> telemetry.end(ending(EndReason.FALL, Optional.of(FALL)), () -> {
            throw failure;
        }));

        assertSame(failure, thrown, "the caller sees the original failure");
        SpanData span = testTelemetry.span("jumprun.end");
        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode(), "status");
        assertEquals(1, span.getEvents().stream().filter(event -> event.getName().equals("exception")).count(), "the exception is recorded");
    }

    @Test
    void anEndCountsByReasonAndMode() {
        telemetry.end(ending(EndReason.FALL, Optional.of(FALL)), () -> false);
        telemetry.end(ending(EndReason.FALL, Optional.of(FALL)), () -> false);
        telemetry.end(ending(EndReason.ABORT, Optional.empty()), () -> false);

        assertEquals(2, testTelemetry.counter("titan.jumprun.runs.ended", ended(EndReason.FALL, Mode.MEDIUM)), "falls");
        assertEquals(1, testTelemetry.counter("titan.jumprun.runs.ended", ended(EndReason.ABORT, Mode.MEDIUM)), "aborts");
    }

    @Test
    void aStartCountsByModeAndOutcome() {
        telemetry.start(PLAYER, Mode.HARD, () -> true);
        telemetry.start(PLAYER, Mode.HARD, () -> false);

        assertEquals(1, testTelemetry.counter("titan.jumprun.runs.started", started(Mode.HARD, "started")), "started");
        assertEquals(1, testTelemetry.counter("titan.jumprun.runs.started", started(Mode.HARD, "no_room")), "no room");
    }

    @Test
    void aStartSpanCarriesModeOutcomeAndPlayer() {
        boolean started = telemetry.start(PLAYER, Mode.HARD, () -> false);

        assertFalse(started, "the attempt's answer is handed back");
        SpanData span = testTelemetry.span("jumprun.start");
        assertEquals("HARD", testTelemetry.attribute(span, AttributeKey.stringKey("jumprun.mode")));
        assertEquals("no_room", testTelemetry.attribute(span, AttributeKey.stringKey("jumprun.start.outcome")));
        assertEquals(PLAYER.toString(), testTelemetry.attribute(span, Telemetry.USER_ID));
    }

    @Test
    void aLeaderboardRefreshSpanCountsTheRunsItUpdated() {
        telemetry.refreshLeaderboard(() -> 3);

        assertEquals(3L, testTelemetry.attribute(testTelemetry.span("jumprun.leaderboard.refresh"), AttributeKey.longKey("jumprun.leaderboard.runs_shown")));
    }

    @Test
    void noMetricCarriesAPlayerAndNothingNamesThePlayer() {
        telemetry.start(PLAYER, Mode.MEDIUM, () -> true);
        telemetry.end(ending(EndReason.FALL, Optional.of(FALL)), () -> true);

        assertEquals(0, testTelemetry.counter("titan.jumprun.runs.ended", ended(EndReason.FALL, Mode.MEDIUM).toBuilder().put(Telemetry.USER_ID, PLAYER.toString()).build()), "no counter point has a user.id");
        assertEquals(1, testTelemetry.counter("titan.jumprun.runs.ended", ended(EndReason.FALL, Mode.MEDIUM)), "the counter carries only reason and mode");
        assertTrue(testTelemetry.allSpanText().stream().noneMatch(text -> text.contains(PLAYER_NAME)), "the name appears in no span name or value");
    }

    @Test
    void theScoreGoesIntoAHistogramPerMode() {
        InMemoryMetricReader reader = InMemoryMetricReader.create();
        try (SdkMeterProvider meters = SdkMeterProvider.builder().registerMetricReader(reader).build()) {
            RunTelemetry own = new RunTelemetry(new Telemetry(testTelemetry.telemetry().tracer(), meters.get("test")));

            own.end(ending(EndReason.FALL, Optional.of(FALL)), () -> false);

            var points = reader.collectAllMetrics().stream().filter(metric -> metric.getName().equals("titan.jumprun.run.score")).flatMap(metric -> metric.getHistogramData().getPoints().stream()).toList();
            assertEquals(1, points.size(), "one point for one mode");
            assertEquals("MEDIUM", points.getFirst().getAttributes().get(AttributeKey.stringKey("mode")), "mode");
            assertEquals(12.0, points.getFirst().getSum(), "the score is recorded");
            assertNull(points.getFirst().getAttributes().get(Telemetry.USER_ID), "no user.id on the histogram");
        }
    }
}
