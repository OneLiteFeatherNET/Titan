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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import java.util.Objects;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Counters of the radio. They carry the kind, mode, trigger or reason, never a player id, and no
 * span is created: starts, stops and dropped notes are too frequent to trace one by one.
 */
final class RadioTelemetry {

    static final AttributeKey<String> KIND = AttributeKey.stringKey("kind");
    static final AttributeKey<String> MODE = AttributeKey.stringKey("mode");
    static final AttributeKey<String> TRIGGER = AttributeKey.stringKey("trigger");
    static final AttributeKey<String> CATEGORY = AttributeKey.stringKey("category");
    static final AttributeKey<String> REASON = AttributeKey.stringKey("reason");
    static final AttributeKey<String> RESULT = AttributeKey.stringKey("result");

    private static final Attributes LOADED = Attributes.of(RESULT, "loaded");
    private static final Attributes REJECTED = Attributes.of(RESULT, "rejected");

    private final LongCounter plays;
    private final LongCounter stops;
    private final LongCounter droppedNotes;
    private final LongCounter songs;

    RadioTelemetry(Telemetry telemetry) {
        Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.plays = telemetry.meter().counterBuilder("titan.radio.plays").setUnit("{start}").setDescription("Titles started, by kind, play mode, trigger and category").build();
        this.stops = telemetry.meter().counterBuilder("titan.radio.stops").setUnit("{stop}").setDescription("Titles stopped, by reason and kind").build();
        this.droppedNotes = telemetry.meter().counterBuilder("titan.radio.notes.dropped").setUnit("{note}").setDescription("MIDI notes dropped for lateness or the notes-per-tick budget").build();
        this.songs = telemetry.meter().counterBuilder("titan.radio.songs").setUnit("{file}").setDescription("MIDI files read at start, by result").build();
    }

    /**
     * The category is its id, which the config bounds to 22 values; stops carry no category, so
     * starts are not counted twice.
     */
    void started(RadioKind kind, PlayMode mode, Trigger trigger, String category) {
        this.plays.add(1, Attributes.builder().put(KIND, kind.telemetryName()).put(MODE, mode.telemetryName()).put(TRIGGER, trigger.telemetryName()).put(CATEGORY, category).build());
    }

    void stopped(StopReason reason, RadioKind kind) {
        this.stops.add(1, Attributes.builder().put(REASON, reason.telemetryName()).put(KIND, kind.telemetryName()).build());
    }

    void notesDropped(int count) {
        if (count > 0) {
            this.droppedNotes.add(count);
        }
    }

    void songLoaded() {
        this.songs.add(1, LOADED);
    }

    void songRejected() {
        this.songs.add(1, REJECTED);
    }
}
