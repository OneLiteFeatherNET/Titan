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
package net.onelitefeather.titan.common.deliver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minestom.server.entity.Player;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.api.deliver.DeliverComponent;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Every test builds its own telemetry and fake deliver (F.I.R.S.T.: independent, repeatable). */
class TracedDeliverTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-00000000a1ec");
    private static final AttributeKey<String> TARGET_TYPE = AttributeKey.stringKey("titan.deliver.target_type");
    private static final AttributeKey<String> TARGET = AttributeKey.stringKey("titan.deliver.target");
    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("titan.deliver.result");

    private final TestTelemetry testTelemetry = TestTelemetry.create();
    private final List<DeliverComponent> sent = new ArrayList<>();
    private final Player player = Mockito.mock(Player.class);

    @AfterEach
    void closeTelemetry() {
        this.testTelemetry.close();
    }

    private TracedDeliver tracedDeliver(Deliver delegate) {
        return new TracedDeliver(delegate, this.testTelemetry.telemetry());
    }

    private static DeliverComponent task(String name) {
        return DeliverComponent.taskBuilder().taskName(name).playerId(PLAYER).build();
    }

    @DisplayName("A delivery to a task is wrapped in a deliver span with its target, player and an ok result")
    @Test
    void aTaskDeliveryIsSpanned() {
        tracedDeliver((player, component) -> this.sent.add(component)).sendPlayer(this.player, task("Survival"));

        SpanData span = this.testTelemetry.span("deliver.send_player");
        assertEquals("task", this.testTelemetry.attribute(span, TARGET_TYPE), "the target type");
        assertEquals("Survival", this.testTelemetry.attribute(span, TARGET), "the task name");
        assertEquals(PLAYER.toString(), this.testTelemetry.attribute(span, Telemetry.USER_ID), "the player's uuid");
        assertEquals("ok", this.testTelemetry.attribute(span, RESULT), "the delivery result");
        assertEquals(StatusCode.UNSET, span.getStatus().getStatusCode(), "a successful delivery is no error");
        assertEquals(1, this.sent.size(), "the wrapped deliver still runs exactly once");
    }

    @DisplayName("A delivery to a game server names the server as its target")
    @Test
    void aServerDeliveryNamesTheServer() {
        DeliverComponent server = DeliverComponent.serverBuilder().serverName("lobby-2").playerId(PLAYER).build();

        tracedDeliver((player, component) -> this.sent.add(component)).sendPlayer(this.player, server);

        SpanData span = this.testTelemetry.span("deliver.send_player");
        assertEquals("server", this.testTelemetry.attribute(span, TARGET_TYPE), "the target type");
        assertEquals("lobby-2", this.testTelemetry.attribute(span, TARGET), "the server name");
    }

    @DisplayName("A failing delivery marks the span as an error, records the exception and reaches the caller unchanged")
    @Test
    void aFailingDeliveryIsMarkedAndRethrown() {
        IllegalStateException failure = new IllegalStateException("message channel is closed");
        TracedDeliver deliver = tracedDeliver((player, component) -> {
            throw failure;
        });

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> deliver.sendPlayer(this.player, task("Survival")));

        assertSame(failure, thrown, "the caller sees the original exception");
        SpanData span = this.testTelemetry.span("deliver.send_player");
        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode(), "the span carries the error status");
        assertEquals("error", this.testTelemetry.attribute(span, RESULT), "the delivery result");
        assertEquals(1, span.getEvents().stream().filter(event -> event.getName().equals("exception")).count(), "the exception is recorded on the span");
    }
}
