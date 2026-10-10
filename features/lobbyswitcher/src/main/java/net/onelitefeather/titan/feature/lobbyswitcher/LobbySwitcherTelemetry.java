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
package net.onelitefeather.titan.feature.lobbyswitcher;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import java.util.UUID;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Spans and counter of the lobby switcher. Opening and clicking are rare and triggered by a player,
 * so both get a span with the player's UUID as {@code user.id}. The counter carries only the
 * result: service names grow with the number of lobbies, and metrics never hold names or UUIDs.
 * The periodic refresh has neither span nor counter.
 */
final class LobbySwitcherTelemetry {

    static final String OPEN_SPAN = "lobbyswitcher.open";
    static final String SELECT_SPAN = "lobbyswitcher.select";
    static final String SELECTIONS = "titan.lobbyswitcher.selections";

    private static final AttributeKey<Long> ENTRIES = AttributeKey.longKey("lobbyswitcher.entries");
    private static final AttributeKey<String> TARGET = AttributeKey.stringKey("lobbyswitcher.target");
    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("lobbyswitcher.result");
    private static final AttributeKey<String> METRIC_RESULT = AttributeKey.stringKey("result");

    private final Telemetry telemetry;
    private final LongCounter selections;

    LobbySwitcherTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
        this.selections = telemetry.meter().counterBuilder(SELECTIONS).setUnit("{selection}").build();
    }

    /** Spans the opening of the list for {@code player}, which shows {@code entries} lobbies. */
    void open(UUID player, int entries, Runnable body) {
        Attributes attributes = Attributes.builder().put(Telemetry.USER_ID, player.toString()).put(ENTRIES, (long) entries).build();
        this.telemetry.inSpan(OPEN_SPAN, attributes, body);
    }

    /** Spans what a click led to; a failed check marks the span as an error. */
    void select(UUID player, String target, SwitcherClickDecision decision, Runnable body) {
        Attributes attributes = Attributes.builder().put(Telemetry.USER_ID, player.toString()).put(TARGET, target).put(RESULT, decision.result()).build();
        this.telemetry.inSpan(SELECT_SPAN, attributes, () -> {
            if (decision == SwitcherClickDecision.ERROR) {
                Span.current().setStatus(StatusCode.ERROR);
            }
            body.run();
            this.selections.add(1, Attributes.of(METRIC_RESULT, decision.result()));
        });
    }
}
