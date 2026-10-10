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
package net.onelitefeather.titan.feature.elytra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Telemetry of {@link ElytraModule}: a span when a glide starts and when it ends with its duration,
 * the flight and boost counters, and no span for the ticks of a flight. Each test builds its own
 * telemetry, clock and node, and drives time through the {@link AdjustableClock}.
 */
@ExtendWith(MicrotusExtension.class)
class ElytraModuleTelemetryTest {

    private static final AttributeKey<String> EVENT = AttributeKey.stringKey("event");
    private static final AttributeKey<Long> DURATION_MS = AttributeKey.longKey("elytra.glide.duration_ms");
    private static final Instant NOW = Instant.parse("2026-10-10T12:00:00Z");

    @DisplayName("Starting to glide opens an elytra.glide.start span carrying the player's UUID")
    @Test
    void startingToGlideOpensAGlideStartSpanWithThePlayer(Env env) {
        AdjustableClock clock = new AdjustableClock(NOW, ZoneOffset.UTC);
        try (TestTelemetry telemetry = TestTelemetry.create(); ElytraFixture fixture = ElytraFixture.start(env, telemetry.telemetry(), clock)) {
            Player player = env.createPlayer(env.createFlatInstance());

            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));

            assertEquals(player.getUuid().toString(), telemetry.attribute(telemetry.span("elytra.glide.start"), Telemetry.USER_ID), "the player's UUID");
        }
    }

    @DisplayName("Landing opens an elytra.glide.end span with the duration measured by the clock")
    @Test
    void landingOpensAGlideEndSpanWithTheMeasuredDuration(Env env) {
        AdjustableClock clock = new AdjustableClock(NOW, ZoneOffset.UTC);
        try (TestTelemetry telemetry = TestTelemetry.create(); ElytraFixture fixture = ElytraFixture.start(env, telemetry.telemetry(), clock)) {
            Player player = env.createPlayer(env.createFlatInstance());
            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));
            clock.advance(Duration.ofSeconds(42));

            env.process().eventHandler().call(new PlayerStopFlyingWithElytraEvent(player));

            SpanData end = telemetry.span("elytra.glide.end");
            assertEquals(42_000L, telemetry.attribute(end, DURATION_MS), "the flight lasted 42 seconds on the injected clock");
            assertEquals(player.getUuid().toString(), telemetry.attribute(end, Telemetry.USER_ID), "the player's UUID");
        }
    }

    @DisplayName("A disconnect in flight forgets the glide, so a later landing opens no end span")
    @Test
    void aDisconnectInFlightForgetsTheGlide(Env env) {
        AdjustableClock clock = new AdjustableClock(NOW, ZoneOffset.UTC);
        try (TestTelemetry telemetry = TestTelemetry.create(); ElytraFixture fixture = ElytraFixture.start(env, telemetry.telemetry(), clock)) {
            Player player = env.createPlayer(env.createFlatInstance());
            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));
            env.process().eventHandler().call(new PlayerDisconnectEvent(player));

            env.process().eventHandler().call(new PlayerStopFlyingWithElytraEvent(player));

            assertTrue(telemetry.spans().stream().noneMatch(span -> span.getName().equals("elytra.glide.end")), "the start was forgotten with the disconnect, so there is no duration to report");
        }
    }

    @DisplayName("Starting and landing count one flight each under the event attribute")
    @Test
    void startingAndLandingCountFlights(Env env) {
        AdjustableClock clock = new AdjustableClock(NOW, ZoneOffset.UTC);
        try (TestTelemetry telemetry = TestTelemetry.create(); ElytraFixture fixture = ElytraFixture.start(env, telemetry.telemetry(), clock)) {
            Player player = env.createPlayer(env.createFlatInstance());

            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));
            env.process().eventHandler().call(new PlayerStopFlyingWithElytraEvent(player));

            assertEquals(1, telemetry.counter("elytra.flights", Attributes.of(EVENT, "started")), "one started flight");
            assertEquals(1, telemetry.counter("elytra.flights", Attributes.of(EVENT, "landed")), "one landed flight");
        }
    }

    @DisplayName("Lighting a firework counts one boost")
    @Test
    void lightingAFireworkCountsOneBoost(Env env) {
        AdjustableClock clock = new AdjustableClock(NOW, ZoneOffset.UTC);
        try (TestTelemetry telemetry = TestTelemetry.create(); ElytraFixture fixture = ElytraFixture.start(env, telemetry.telemetry(), clock)) {
            Player player = env.createPlayer(env.createFlatInstance());
            player.setFlyingWithElytra(true);

            fixture.useFirework(player);

            assertEquals(1, telemetry.counter("elytra.boosts", Attributes.empty()), "one lit firework");
        }
    }

    @DisplayName("A refused firework during the cooldown counts no boost")
    @Test
    void aRefusedFireworkCountsNoBoost(Env env) {
        AdjustableClock clock = new AdjustableClock(NOW, ZoneOffset.UTC);
        try (TestTelemetry telemetry = TestTelemetry.create(); ElytraFixture fixture = ElytraFixture.start(env, telemetry.telemetry(), clock)) {
            Player player = env.createPlayer(env.createFlatInstance());
            player.setFlyingWithElytra(true);
            fixture.useFirework(player);

            fixture.useFirework(player);

            assertEquals(1, telemetry.counter("elytra.boosts", Attributes.empty()), "the second use was refused, so it is not counted");
        }
    }

    @DisplayName("The hundred ticks of a flight open no span of their own")
    @Test
    void theTicksOfAFlightOpenNoSpan(Env env) {
        AdjustableClock clock = new AdjustableClock(NOW, ZoneOffset.UTC);
        try (TestTelemetry telemetry = TestTelemetry.create(); ElytraFixture fixture = ElytraFixture.start(env, telemetry.telemetry(), clock)) {
            Player player = env.createPlayer(env.createFlatInstance());
            player.setFlyingWithElytra(true);
            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));
            int spansBeforeTheTicks = telemetry.spans().size();

            for (int tick = 0; tick < 100; tick++) {
                // Reasserted every tick, as in ElytraModuleTest: physics would otherwise land the player.
                player.setFlyingWithElytra(true);
                env.tick();
            }

            assertEquals(spansBeforeTheTicks, telemetry.spans().size(), "no span may be opened by the ticks of a flight");
        }
    }
}
