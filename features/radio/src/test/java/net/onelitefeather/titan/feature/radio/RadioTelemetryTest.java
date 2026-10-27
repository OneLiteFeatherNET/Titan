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
package net.onelitefeather.titan.feature.radio;

import io.opentelemetry.api.common.Attributes;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RadioTelemetryTest {

    private static final String PLAYS = "titan.radio.plays";
    private static final String STOPS = "titan.radio.stops";
    private static final String DROPPED = "titan.radio.notes.dropped";
    private static final String SONGS = "titan.radio.songs";

    @DisplayName("A start counts by kind, mode and trigger")
    @Test
    void startCountsByKindModeTriggerAndCategory() {
        try (TestTelemetry test = TestTelemetry.create()) {
            RadioTelemetry telemetry = new RadioTelemetry(test.telemetry());

            telemetry.started(RadioKind.VANILLA, PlayMode.LOOP, Trigger.AUTO, "minecraft");

            Assertions.assertEquals(1, test.counter(PLAYS, Attributes.builder().put(RadioTelemetry.KIND, "vanilla").put(RadioTelemetry.MODE, "loop").put(RadioTelemetry.TRIGGER, "auto").put(RadioTelemetry.CATEGORY, "minecraft").build()), "one vanilla start in loop, triggered automatically, must be counted once with its category");
        }
    }

    @DisplayName("A start in a declared category counts with the category id, never a player id")
    @Test
    void startCountsWithDeclaredCategoryId() {
        try (TestTelemetry test = TestTelemetry.create()) {
            RadioTelemetry telemetry = new RadioTelemetry(test.telemetry());

            telemetry.started(RadioKind.MIDI, PlayMode.ONCE, Trigger.USER, "games");

            Assertions.assertEquals(1, test.counter(PLAYS, Attributes.builder().put(RadioTelemetry.KIND, "midi").put(RadioTelemetry.MODE, "once").put(RadioTelemetry.TRIGGER, "user").put(RadioTelemetry.CATEGORY, "games").build()), "a MIDI start in games must be counted with the category id games");
        }
    }

    @DisplayName("A stop counts by reason and kind")
    @Test
    void stopCountsByReasonAndKind() {
        try (TestTelemetry test = TestTelemetry.create()) {
            RadioTelemetry telemetry = new RadioTelemetry(test.telemetry());

            telemetry.stopped(StopReason.USER, RadioKind.MIDI);

            Assertions.assertEquals(1, test.counter(STOPS, Attributes.builder().put(RadioTelemetry.REASON, "user").put(RadioTelemetry.KIND, "midi").build()), "a user stop of a MIDI song must be counted once");
        }
    }

    @DisplayName("A title that ends into a successor counts an end stop and an automatic start")
    @Test
    void endIntoSuccessorCountsBoth() {
        try (TestTelemetry test = TestTelemetry.create()) {
            RadioTelemetry telemetry = new RadioTelemetry(test.telemetry());

            telemetry.stopped(StopReason.END, RadioKind.VANILLA);
            telemetry.started(RadioKind.VANILLA, PlayMode.SHUFFLE, Trigger.AUTO, "minecraft");

            Assertions.assertEquals(1, test.counter(STOPS, Attributes.builder().put(RadioTelemetry.REASON, "end").put(RadioTelemetry.KIND, "vanilla").build()), "the end must count as an end stop");
            Assertions.assertEquals(1, test.counter(PLAYS, Attributes.builder().put(RadioTelemetry.KIND, "vanilla").put(RadioTelemetry.MODE, "shuffle").put(RadioTelemetry.TRIGGER, "auto").put(RadioTelemetry.CATEGORY, "minecraft").build()), "the successor must count as an automatic start");
        }
    }

    @DisplayName("Dropped notes add up in one counter without attributes")
    @Test
    void droppedNotesAddUp() {
        try (TestTelemetry test = TestTelemetry.create()) {
            RadioTelemetry telemetry = new RadioTelemetry(test.telemetry());

            telemetry.notesDropped(3);
            telemetry.notesDropped(2);

            Assertions.assertEquals(5, test.counter(DROPPED, Attributes.empty()), "the dropped notes of both ticks must add up");
        }
    }

    @DisplayName("Loaded and rejected MIDI files count by result")
    @Test
    void songResultsCountByResult() {
        try (TestTelemetry test = TestTelemetry.create()) {
            RadioTelemetry telemetry = new RadioTelemetry(test.telemetry());

            telemetry.songLoaded();
            telemetry.songRejected();
            telemetry.songRejected();

            Assertions.assertEquals(1, test.counter(SONGS, Attributes.builder().put(RadioTelemetry.RESULT, "loaded").build()), "one loaded song must be counted as loaded");
            Assertions.assertEquals(2, test.counter(SONGS, Attributes.builder().put(RadioTelemetry.RESULT, "rejected").build()), "two rejected songs must be counted as rejected");
        }
    }
}
