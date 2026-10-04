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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.trace.Span;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Names and attributes of the spans and metrics a jump and run produces. Only rare operations get
 * a span (a start, an end, a leaderboard refresh), never a whole run, a tick or a move. Metrics
 * carry small enums only; the player is a span attribute, as a UUID.
 */
final class RunTelemetry {

    static final String END_SPAN = "jumprun.end";
    static final String START_SPAN = "jumprun.start";
    static final String REFRESH_SPAN = "jumprun.leaderboard.refresh";

    private static final AttributeKey<String> END_REASON = AttributeKey.stringKey("jumprun.end.reason");
    private static final AttributeKey<String> MODE = AttributeKey.stringKey("jumprun.mode");
    private static final AttributeKey<Long> SCORE = AttributeKey.longKey("jumprun.score");
    private static final AttributeKey<Boolean> RECORD = AttributeKey.booleanKey("jumprun.record");
    private static final AttributeKey<Double> FALL_Y = AttributeKey.doubleKey("jumprun.fall.y");
    private static final AttributeKey<Double> FALL_THRESHOLD = AttributeKey.doubleKey("jumprun.fall.threshold");
    private static final AttributeKey<Long> FALL_COURSE_INDEX = AttributeKey.longKey("jumprun.fall.course_index");
    private static final AttributeKey<String> START_OUTCOME = AttributeKey.stringKey("jumprun.start.outcome");
    private static final AttributeKey<Long> RUNS_SHOWN = AttributeKey.longKey("jumprun.leaderboard.runs_shown");

    private static final AttributeKey<String> METRIC_REASON = AttributeKey.stringKey("reason");
    private static final AttributeKey<String> METRIC_MODE = AttributeKey.stringKey("mode");
    private static final AttributeKey<String> METRIC_OUTCOME = AttributeKey.stringKey("outcome");

    private static final String STARTED = "started";
    private static final String NO_ROOM = "no_room";

    /** Where and how far below the course a run fell. */
    record Fall(double y, double threshold, int courseIndex) {
    }

    record Ending(UUID player, Mode mode, EndReason reason, int score, Optional<Fall> fall) {
    }

    private final Telemetry telemetry;
    private final LongCounter ended;
    private final LongCounter started;
    private final LongHistogram score;

    RunTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
        this.ended = telemetry.meter().counterBuilder("titan.jumprun.runs.ended").setUnit("{run}").build();
        this.started = telemetry.meter().counterBuilder("titan.jumprun.runs.started").setUnit("{run}").build();
        this.score = telemetry.meter().histogramBuilder("titan.jumprun.run.score").ofLongs().setUnit("{point}").build();
    }

    /**
     * Runs {@code submit} inside the end span, so the database spans of the stored run hang under
     * it.
     *
     * @return whether the run was a record, as {@code submit} said
     */
    boolean end(Ending ending, BooleanSupplier submit) {
        this.ended.add(1, Attributes.of(METRIC_REASON, ending.reason().name(), METRIC_MODE, ending.mode().name()));
        this.score.record(ending.score(), Attributes.of(METRIC_MODE, ending.mode().name()));
        AttributesBuilder attributes = Attributes.builder().put(Telemetry.USER_ID, ending.player().toString()).put(END_REASON, ending.reason().name()).put(MODE, ending.mode().name()).put(SCORE, (long) ending.score());
        ending.fall().ifPresent(fall -> attributes.put(FALL_Y, fall.y()).put(FALL_THRESHOLD, fall.threshold()).put(FALL_COURSE_INDEX, (long) fall.courseIndex()));
        return this.telemetry.inSpan(END_SPAN, attributes.build(), () -> {
            boolean record = submit.getAsBoolean();
            Span.current().setAttribute(RECORD, record);
            return record;
        });
    }

    /** @return whether the run began, as {@code attempt} said */
    boolean start(UUID player, Mode mode, BooleanSupplier attempt) {
        Attributes attributes = Attributes.of(Telemetry.USER_ID, player.toString(), MODE, mode.name());
        return this.telemetry.inSpan(START_SPAN, attributes, () -> {
            boolean began = attempt.getAsBoolean();
            String outcome = began ? STARTED : NO_ROOM;
            Span.current().setAttribute(START_OUTCOME, outcome);
            this.started.add(1, Attributes.of(METRIC_MODE, mode.name(), METRIC_OUTCOME, outcome));
            return began;
        });
    }

    /** {@code refresh} returns how many runs got their sidebar updated. */
    void refreshLeaderboard(IntSupplier refresh) {
        this.telemetry.inSpan(REFRESH_SPAN, Attributes.empty(), () -> {
            Span.current().setAttribute(RUNS_SHOWN, (long) refresh.getAsInt());
        });
    }
}
