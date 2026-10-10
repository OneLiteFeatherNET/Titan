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
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import net.minestom.server.entity.Player;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Spans and the selection counter of the lobby switcher. */
@ExtendWith(MicrotusExtension.class)
class LobbySwitcherTelemetryTest {

    private static final AttributeKey<Long> ENTRIES = AttributeKey.longKey("lobbyswitcher.entries");
    private static final AttributeKey<String> TARGET = AttributeKey.stringKey("lobbyswitcher.target");
    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("lobbyswitcher.result");
    private static final AttributeKey<String> METRIC_RESULT = AttributeKey.stringKey("result");
    private static final String COUNTER = "titan.lobbyswitcher.selections";

    private final TestTelemetry testTelemetry = TestTelemetry.create();

    @AfterEach
    void closeTelemetry() {
        this.testTelemetry.close();
    }

    private SwitcherFixture fixture(Env env) {
        return SwitcherFixture.active(env, this.testTelemetry.telemetry());
    }

    private SpanData selectAfterClick(Env env, SwitcherFixture fixture, int slot) {
        Player player = fixture.joinAndOpen();
        fixture.click(player, slot);
        env.tick();
        return this.testTelemetry.span("lobbyswitcher.select");
    }

    @DisplayName("Opening spans lobbyswitcher.open with the player and the number of entries")
    @Test
    void openingSpansUserAndEntries(Env env) {
        try (SwitcherFixture fixture = fixture(env)) {
            Player player = fixture.joinAndOpen();

            SpanData span = this.testTelemetry.span("lobbyswitcher.open");
            Assertions.assertEquals(player.getUuid().toString(), this.testTelemetry.attribute(span, Telemetry.USER_ID), "the player UUID is the user.id");
            Assertions.assertEquals(3L, this.testTelemetry.attribute(span, ENTRIES), "three services are listed");
        }
    }

    @DisplayName("The periodic refresh creates no span")
    @Test
    void refreshCreatesNoSpan(Env env) {
        try (SwitcherFixture fixture = fixture(env)) {
            fixture.joinAndOpen();
            int before = this.testTelemetry.spans().size();

            for (int i = 0; i < SwitcherFixture.REFRESH_SECONDS * ViewerCounter.TICKS_PER_SECOND + 10; i++) {
                env.tick();
            }

            Assertions.assertEquals(2, fixture.counts().reads(), "precondition: a refresh did happen");
            Assertions.assertEquals(before, this.testTelemetry.spans().size(), "a refresh must not span");
        }
    }

    @DisplayName("A delivered click spans select with player, target and result sent")
    @Test
    void sentClickSpans(Env env) {
        try (SwitcherFixture fixture = fixture(env)) {
            SpanData span = selectAfterClick(env, fixture, 0);

            Assertions.assertEquals("Lobby-1", this.testTelemetry.attribute(span, TARGET), "the service name is the target");
            Assertions.assertEquals("sent", this.testTelemetry.attribute(span, RESULT), "a joinable lobby is sent");
            Assertions.assertNotNull(this.testTelemetry.attribute(span, Telemetry.USER_ID), "the span carries the user.id");
            Assertions.assertEquals(StatusCode.UNSET, span.getStatus().getStatusCode(), "a sent click is not an error");
        }
    }

    @DisplayName("A click on the own lobby reports current")
    @Test
    void currentClickSpans(Env env) {
        try (SwitcherFixture fixture = fixture(env)) {
            Assertions.assertEquals("current", this.testTelemetry.attribute(selectAfterClick(env, fixture, 1), RESULT));
        }
    }

    @DisplayName("A click on a full lobby reports full")
    @Test
    void fullClickSpans(Env env) {
        try (SwitcherFixture fixture = fixture(env)) {
            Assertions.assertEquals("full", this.testTelemetry.attribute(selectAfterClick(env, fixture, 2), RESULT));
        }
    }

    @DisplayName("A click on a lobby without a limit reports not_ready")
    @Test
    void notReadyClickSpans(Env env) {
        try (SwitcherFixture fixture = fixture(env)) {
            fixture.counts().serve(new ServiceCount("Lobby-1", 0, 0), SwitcherFixture.LOBBY_2);

            Assertions.assertEquals("not_ready", this.testTelemetry.attribute(selectAfterClick(env, fixture, 0), RESULT));
        }
    }

    @DisplayName("A click on a lobby that stopped reports gone")
    @Test
    void goneClickSpans(Env env) {
        try (SwitcherFixture fixture = fixture(env)) {
            Player player = fixture.joinAndOpen();
            fixture.counts().serve(SwitcherFixture.LOBBY_2);
            fixture.click(player, 0);
            env.tick();

            Assertions.assertEquals("gone", this.testTelemetry.attribute(this.testTelemetry.span("lobbyswitcher.select"), RESULT));
        }
    }

    @DisplayName("A failing check reports error and marks the span as ERROR")
    @Test
    void errorClickSetsSpanStatus(Env env) {
        try (SwitcherFixture fixture = fixture(env)) {
            Player player = fixture.joinAndOpen();
            fixture.counts().failWith(new IllegalStateException("provider down"));
            fixture.click(player, 0);
            env.tick();

            SpanData span = this.testTelemetry.span("lobbyswitcher.select");
            Assertions.assertEquals("error", this.testTelemetry.attribute(span, RESULT), "the result is error");
            Assertions.assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode(), "the span status is ERROR");
        }
    }

    @DisplayName("The selection counter carries only the result")
    @Test
    void counterCarriesOnlyTheResult(Env env) {
        try (SwitcherFixture fixture = fixture(env)) {
            selectAfterClick(env, fixture, 0);

            Assertions.assertEquals(1, this.testTelemetry.counter(COUNTER, Attributes.of(METRIC_RESULT, "sent")), "counted with the result attribute alone");
        }
    }

    @DisplayName("The counter has no point that carries a service name")
    @Test
    void counterHasNoServiceName(Env env) {
        try (SwitcherFixture fixture = fixture(env)) {
            selectAfterClick(env, fixture, 2);

            Assertions.assertEquals(0, this.testTelemetry.counter(COUNTER, Attributes.of(METRIC_RESULT, "full", TARGET, "Lobby-3")), "no target attribute on the counter");
            Assertions.assertEquals(1, this.testTelemetry.counter(COUNTER, Attributes.of(METRIC_RESULT, "full")), "the counter holds the result only");
        }
    }
}
