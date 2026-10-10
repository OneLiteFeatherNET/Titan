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
package net.onelitefeather.titan.feature.season;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
import java.util.Locale;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * The span, event and metrics of the periodic season check. The check runs once a minute, so it
 * gets one span; a restart request is rare and rides along on that span as an event.
 */
final class SeasonTelemetry {

    static final String CHECK_SPAN = "season.check";
    static final String STOP_REQUESTED_EVENT = "season.stop_requested";

    /** The world name reported when the lobby runs the default world. */
    static final String DEFAULT_WORLD = "default";
    /** The desired world when the configuration cannot be read. */
    static final String UNRESOLVABLE_WORLD = "unresolvable";

    private static final AttributeKey<String> CURRENT = AttributeKey.stringKey("season.current");
    private static final AttributeKey<String> DESIRED = AttributeKey.stringKey("season.desired");
    private static final AttributeKey<String> OUTCOME = AttributeKey.stringKey("season.outcome");
    private static final AttributeKey<Long> ONLINE_PLAYERS = AttributeKey.longKey("season.online_players");

    /**
     * The result of one check; the span and the {@code season.checks} counter use its wire name.
     */
    enum Outcome {
        UNCHANGED, PENDING_RESTART, RESTART_REQUESTED, UNRESOLVABLE;

        String wireName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final Telemetry telemetry;
    private final LongCounter checks;
    private final LongCounter restartsRequested;

    SeasonTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
        this.checks = telemetry.meter().counterBuilder("season.checks").setUnit("{check}").setDescription("Periodic season checks by outcome").build();
        this.restartsRequested = telemetry.meter().counterBuilder("season.restarts_requested").setUnit("{restart}").setDescription("Lobby stops requested for a season change").build();
    }

    /** Runs one check inside its span; {@code current} is the world the lobby runs in now. */
    void inCheck(String current, Runnable check) {
        this.telemetry.inSpan(CHECK_SPAN, Attributes.of(CURRENT, current), check);
    }

    void recordOutcome(String desired, int onlinePlayers, Outcome outcome) {
        Span span = Span.current();
        span.setAttribute(DESIRED, desired);
        span.setAttribute(ONLINE_PLAYERS, (long) onlinePlayers);
        span.setAttribute(OUTCOME, outcome.wireName());
        this.checks.add(1, Attributes.of(OUTCOME, outcome.wireName()));
    }

    /** Called once per actual stop request, not for later checks that repeat it. */
    void restartRequested() {
        Span.current().addEvent(STOP_REQUESTED_EVENT);
        this.restartsRequested.add(1);
    }
}
