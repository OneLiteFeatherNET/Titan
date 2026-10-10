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
package net.onelitefeather.titan.feature.spawn;

import io.avaje.config.Config;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayList;
import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.LobbyHeightBounds;
import net.onelitefeather.titan.core.module.item.LobbyItems;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * Tracing of the spawn column on a real {@code Env}: a height-bounds teleport produces one span,
 * moves inside the bounds produce none, and a join produces a span that records a failure.
 */
@ExtendWith(MicrotusExtension.class)
class SpawnTelemetryTest {

    private static final int MIN_HEIGHT = Config.getAs(SpawnSettings.MIN_HEIGHT_KEY, Integer::parseInt);
    private static final int MAX_HEIGHT = Config.getAs(SpawnSettings.MAX_HEIGHT_KEY, Integer::parseInt);
    private static final Pos SPAWN = new Pos(10, 100, 10);

    private final TestTelemetry testTelemetry = TestTelemetry.create();

    @AfterEach
    void closeTelemetry() {
        testTelemetry.close();
    }

    @DisplayName("Falling below the minimum height produces exactly one spawn.bounds_teleport span with the bounds")
    @Test
    void fallingBelowMinHeightProducesOneBoundsTeleportSpan(Env env) {
        Instance instance = env.createFlatInstance();
        LobbyHeightBounds bounds = new SpawnHeightBoundsFactory().lobbyHeightBounds();
        Player player = env.createPlayer(instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SpawnModule module = new SpawnModule(instance, () -> SPAWN, bounds, titan.node(), Mockito.mock(LobbyItems.class), testTelemetry.telemetry());
            module.start();
            try {
                Pos belowMin = new Pos(0, MIN_HEIGHT - 10, 0);
                player.teleport(belowMin);

                env.process().eventHandler().call(new PlayerMoveEvent(player, belowMin, true));

                SpanData span = testTelemetry.span("spawn.bounds_teleport");
                Assertions.assertEquals(MIN_HEIGHT - 10.0, testTelemetry.attribute(span, SpawnTelemetry.Y), "the height the player fell to");
                Assertions.assertEquals((long) MIN_HEIGHT, testTelemetry.attribute(span, SpawnTelemetry.MIN_HEIGHT), "the configured minimum");
                Assertions.assertEquals((long) MAX_HEIGHT, testTelemetry.attribute(span, SpawnTelemetry.MAX_HEIGHT), "the configured maximum");
                Assertions.assertEquals(1, testTelemetry.counter("spawn.bounds_teleports", Attributes.empty()), "the teleport is counted once");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Moves inside the height bounds produce no span and no bounds teleport counter")
    @Test
    void movesInsideBoundsProduceNoSpan(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        Pos inside = new Pos(0, (MIN_HEIGHT + MAX_HEIGHT) / 2.0, 0);
        player.teleport(inside);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SpawnModule module = new SpawnModule(instance, () -> SPAWN, new SpawnHeightBoundsFactory().lobbyHeightBounds(), titan.node(), Mockito.mock(LobbyItems.class), testTelemetry.telemetry());
            module.start();
            try {
                for (int move = 0; move < 100; move++) {
                    env.process().eventHandler().call(new PlayerMoveEvent(player, inside, true));
                }

                Assertions.assertTrue(testTelemetry.spans().isEmpty(), "a move inside the bounds must not produce a span, found " + testTelemetry.spans().stream().map(SpanData::getName).toList());
                Assertions.assertEquals(0, testTelemetry.counter("spawn.bounds_teleports", Attributes.empty()), "no bounds teleport was counted");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A join produces a spawn.join span that carries the player's UUID")
    @Test
    void aJoinProducesASpawnJoinSpan(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SpawnModule module = new SpawnModule(instance, () -> SPAWN, new SpawnHeightBoundsFactory().lobbyHeightBounds(), titan.node(), Mockito.mock(LobbyItems.class), testTelemetry.telemetry());
            module.start();
            try {
                env.process().eventHandler().call(new PlayerSpawnEvent(player, instance, true));

                SpanData span = testTelemetry.span("spawn.join");
                Assertions.assertEquals(player.getUuid().toString(), testTelemetry.attribute(span, Telemetry.USER_ID), "the player's UUID");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A join whose spawn teleport throws records the exception and status ERROR on spawn.join")
    @Test
    void aFailingJoinMarksTheJoinSpanAsError(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        List<Throwable> serverExceptions = new ArrayList<>();
        env.process().exception().setExceptionHandler(serverExceptions::add);
        IllegalStateException failure = new IllegalStateException("spawn is gone");

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SpawnModule module = new SpawnModule(instance, () -> {
                throw failure;
            }, new SpawnHeightBoundsFactory().lobbyHeightBounds(), titan.node(), Mockito.mock(LobbyItems.class), testTelemetry.telemetry());
            module.start();
            try {
                env.process().eventHandler().call(new PlayerSpawnEvent(player, instance, true));

                SpanData span = testTelemetry.span("spawn.join");
                Assertions.assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode(), "the join span must record the failure");
                Assertions.assertEquals(1, span.getEvents().stream().filter(event -> event.getName().equals("exception")).count(), "the exception is recorded on the span");
                Assertions.assertTrue(serverExceptions.contains(failure), "the failure still reaches the guard, as before");
            } finally {
                module.stop();
            }
        }
    }
}
