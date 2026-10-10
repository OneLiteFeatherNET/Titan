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
package net.onelitefeather.titan.feature.navigator;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
import java.util.function.Supplier;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Spans and counter of the navigator. Opening and clicking are rare and triggered by a player, so
 * both get a span; the counter carries only the destination's name and the outcome.
 */
final class NavigatorTelemetry {

    static final String OPEN_SPAN = "navigator.open";
    static final String SELECT_SPAN = "navigator.select";
    static final String LAYOUT_SPAN = "navigator.layout.apply";

    /** The spawn entry is not a {@link Destination}, so it reports this value instead. */
    static final String SPAWN_DESTINATION = "spawn";

    private static final AttributeKey<String> KIND = AttributeKey.stringKey("navigator.kind");
    private static final AttributeKey<Long> ENTRIES = AttributeKey.longKey("navigator.entries");
    private static final AttributeKey<String> DESTINATION = AttributeKey.stringKey("navigator.destination");
    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("navigator.result");

    private static final AttributeKey<String> METRIC_DESTINATION = AttributeKey.stringKey("destination");
    private static final AttributeKey<String> METRIC_RESULT = AttributeKey.stringKey("result");

    /** What a click on a navigator entry led to. */
    enum Selection {
        SENT("sent"), DENIED("denied"), SPAWN("spawn");

        private final String value;

        Selection(String value) {
            this.value = value;
        }
    }

    private final Telemetry telemetry;
    private final LongCounter selections;

    NavigatorTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
        this.selections = telemetry.meter().counterBuilder("titan.navigator.selections").setUnit("{selection}").build();
    }

    void open(boolean team, int entries, Runnable body) {
        Attributes attributes = Attributes.builder().put(KIND, team ? "team" : "public").put(ENTRIES, (long) entries).build();
        this.telemetry.inSpan(OPEN_SPAN, attributes, body);
    }

    void select(String destination, Supplier<Selection> body) {
        this.telemetry.inSpan(SELECT_SPAN, Attributes.of(DESTINATION, destination), () -> {
            Selection selection = body.get();
            Span.current().setAttribute(RESULT, selection.value);
            this.selections.add(1, Attributes.of(METRIC_DESTINATION, destination, METRIC_RESULT, selection.value));
        });
    }
}
