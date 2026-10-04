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
package net.onelitefeather.titan.core.module;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerChunkLoadEvent;
import net.minestom.server.event.player.PlayerChunkUnloadEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.event.player.PlayerTickEndEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MicrotusExtension.class)
class FeatureNodeTracingTest {

    private record TestEvent() implements Event {
    }

    private static final class TestCancellableEvent implements CancellableEvent {

        private boolean cancelled;

        @Override
        public boolean isCancelled() {
            return this.cancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            this.cancelled = cancel;
        }
    }

    private record PlayerTestEvent(Player player) implements PlayerEvent {

        @Override
        public Player getPlayer() {
            return this.player;
        }
    }

    private final TestTelemetry test = TestTelemetry.create();

    @AfterEach
    void cleanUp() {
        this.test.close();
        ListenerGuard.consumeFailingPlayer();
        ListenerGuard.consumeFailingModule();
    }

    /** Cyano fails a test on any server exception; a listener failure is the subject here. */
    private static List<Throwable> captureServerExceptions(Env env) {
        List<Throwable> captured = new ArrayList<>();
        env.process().exception().setExceptionHandler(captured::add);
        return captured;
    }

    private static Attributes feature(String id) {
        return Attributes.of(Telemetry.FEATURE, id);
    }

    @DisplayName("onTraced runs the listener in a span that carries the feature id")
    @Test
    void onTracedRunsTheListenerInASpanWithTheFeatureId() {
        EventNode<Event> parent = EventNode.all("test-traced");
        String[] insideSpanId = new String[1];

        try (FeatureNode node = FeatureNode.attach(parent, "sit", 500, this.test.telemetry())) {
            node.onTraced(TestEvent.class, "sit.toggle", event -> insideSpanId[0] = Span.current().getSpanContext().getSpanId());
            parent.call(new TestEvent());
        }

        SpanData span = this.test.span("sit.toggle");
        Assertions.assertEquals(span.getSpanId(), insideSpanId[0], "the listener must run inside the span");
        Assertions.assertEquals("sit", this.test.attribute(span, Telemetry.FEATURE));
        Assertions.assertNull(this.test.attribute(span, Telemetry.USER_ID), "an event without a player has no user.id");
    }

    @DisplayName("onTraced puts the player's uuid, and nothing else about the player, on the span")
    @Test
    void onTracedCarriesOnlyThePlayersUuid(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        EventNode<Event> parent = EventNode.all("test-traced-player");

        try (FeatureNode node = FeatureNode.attach(parent, "sit", 500, this.test.telemetry())) {
            node.onTraced(PlayerTestEvent.class, "sit.toggle", event -> {
            });
            parent.call(new PlayerTestEvent(player));
        }

        SpanData span = this.test.span("sit.toggle");
        Assertions.assertEquals(player.getUuid().toString(), this.test.attribute(span, Telemetry.USER_ID));
        Assertions.assertTrue(this.test.allSpanText().stream().noneMatch(text -> text.contains(player.getUsername())), "no span text may contain the player name");
    }

    @DisplayName("A throwing onTraced listener leaves the exception and status ERROR on its span")
    @Test
    void aThrowingTracedListenerMarksItsSpan(Env env) {
        List<Throwable> serverExceptions = captureServerExceptions(env);
        EventNode<Event> parent = EventNode.all("test-traced-throw");

        try (FeatureNode node = FeatureNode.attach(parent, "sit", 500, this.test.telemetry())) {
            node.onTraced(TestEvent.class, "sit.toggle", event -> {
                throw new IllegalStateException("boom");
            });
            parent.call(new TestEvent());
        }

        Assertions.assertEquals(1, serverExceptions.size(), "the failure must still reach Minestom's exception handler");
        SpanData span = this.test.span("sit.toggle");
        Assertions.assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
        Assertions.assertTrue(span.getEvents().stream().map(EventData::getName).anyMatch("exception"::equals), "the span must carry the exception");
    }

    @DisplayName("A throwing onTraced listener increments titan.listener.failures for its feature")
    @Test
    void aThrowingTracedListenerCountsAsAFailure(Env env) {
        List<Throwable> serverExceptions = captureServerExceptions(env);
        EventNode<Event> parent = EventNode.all("test-traced-count");

        try (FeatureNode node = FeatureNode.attach(parent, "sit", 500, this.test.telemetry())) {
            node.onTraced(TestEvent.class, "sit.toggle", event -> {
                throw new IllegalStateException("boom");
            });
            parent.call(new TestEvent());
        }

        Assertions.assertEquals(1, this.test.counter("titan.listener.failures", feature("sit")));
    }

    @DisplayName("A throwing onTraced listener still reaches the guard, which records the feature for the log")
    @Test
    void aThrowingTracedListenerReachesTheGuard(Env env) {
        List<Throwable> serverExceptions = captureServerExceptions(env);
        EventNode<Event> parent = EventNode.all("test-traced-guard");

        try (FeatureNode node = FeatureNode.attach(parent, "sit", 500, this.test.telemetry())) {
            node.onTraced(TestEvent.class, "sit.toggle", event -> {
                throw new IllegalStateException("boom");
            });
            parent.call(new TestEvent());
        }

        Assertions.assertEquals("sit", ListenerGuard.consumeFailingModule(), "the guard must have seen the failure of feature sit");
    }

    @DisplayName("A throwing on() listener increments titan.listener.failures and creates no span")
    @Test
    void aThrowingPlainListenerCountsWithoutASpan(Env env) {
        List<Throwable> serverExceptions = captureServerExceptions(env);
        EventNode<Event> parent = EventNode.all("test-plain-count");

        try (FeatureNode node = FeatureNode.attach(parent, "sit", 500, this.test.telemetry())) {
            node.on(TestEvent.class, event -> {
                throw new IllegalStateException("boom");
            });
            parent.call(new TestEvent());
        }

        Assertions.assertEquals(1, this.test.counter("titan.listener.failures", feature("sit")));
        Assertions.assertTrue(this.test.spans().isEmpty(), "a plain listener must not create a span");
    }

    @DisplayName("A listener that does not throw leaves titan.listener.failures at zero")
    @Test
    void aHealthyListenerIsNotCounted() {
        EventNode<Event> parent = EventNode.all("test-healthy");

        try (FeatureNode node = FeatureNode.attach(parent, "sit", 500, this.test.telemetry())) {
            node.on(TestEvent.class, event -> {
            });
            parent.call(new TestEvent());
        }

        Assertions.assertEquals(0, this.test.counter("titan.listener.failures", feature("sit")));
    }

    @DisplayName("onTracedIncludingCancelled runs for an already cancelled event, in a span")
    @Test
    void onTracedIncludingCancelledSeesCancelledEvents() {
        EventNode<Event> parent = EventNode.all("test-traced-cancelled");
        List<TestCancellableEvent> received = new ArrayList<>();

        try (FeatureNode node = FeatureNode.attach(parent, "navigator", 400, this.test.telemetry())) {
            node.onTracedIncludingCancelled(TestCancellableEvent.class, "navigator.open", received::add);
            TestCancellableEvent event = new TestCancellableEvent();
            event.setCancelled(true);
            parent.call(event);
        }

        Assertions.assertEquals(1, received.size(), "the cancelled event must be delivered");
        Assertions.assertEquals(1, this.test.spans().size(), "and traced");
    }

    static Stream<Class<? extends Event>> highFrequencyEvents() {
        return Stream.of(PlayerMoveEvent.class, PlayerPacketEvent.class, PlayerPacketOutEvent.class, PlayerChunkLoadEvent.class, PlayerChunkUnloadEvent.class, EntityTickEvent.class, InstanceTickEvent.class, ServerTickMonitorEvent.class, PlayerTickEvent.class, PlayerTickEndEvent.class);
    }

    @DisplayName("onTraced refuses a high-frequency event type at registration")
    @ParameterizedTest(name = "{0}")
    @MethodSource("highFrequencyEvents")
    void onTracedRefusesHighFrequencyEvents(Class<? extends Event> type) {
        EventNode<Event> parent = EventNode.all("test-traced-refuse");

        try (FeatureNode node = FeatureNode.attach(parent, "sit", 500, this.test.telemetry())) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> node.onTraced(type, "sit.op", event -> {
            }), type.getSimpleName() + " must be refused");
            Assertions.assertThrows(IllegalArgumentException.class, () -> node.onTracedIncludingCancelled(type, "sit.op", event -> {
            }), type.getSimpleName() + " must be refused by the including-cancelled variant too");
            Assertions.assertTrue(parent.getChildren().iterator().next().getChildren().isEmpty(), "no listener may have been registered");
        }
    }

    @DisplayName("The three-argument attach keeps working and records nothing")
    @Test
    void theOldAttachStillWorks() {
        EventNode<Event> parent = EventNode.all("test-old-attach");
        List<TestEvent> received = new ArrayList<>();

        try (FeatureNode node = FeatureNode.attach(parent, "sit", 500)) {
            node.on(TestEvent.class, received::add);
            node.onTraced(TestEvent.class, "sit.op", received::add);
            parent.call(new TestEvent());
        }

        Assertions.assertEquals(2, received.size(), "both listeners must run with the no-op telemetry");
    }

    @DisplayName("attach adds a feature.started event to the current span")
    @Test
    void attachAddsAFeatureStartedEvent() {
        EventNode<Event> parent = EventNode.all("test-started");

        this.test.telemetry().inSpan("startup", Attributes.empty(), () -> FeatureNode.attach(parent, "sit", 500, this.test.telemetry()));

        EventData started = this.test.span("startup").getEvents().getFirst();
        Assertions.assertEquals("feature.started", started.getName());
        Assertions.assertEquals("sit", started.getAttributes().get(Telemetry.FEATURE));
        Assertions.assertEquals(500L, started.getAttributes().get(Telemetry.FEATURE_PRIORITY));
    }

    @DisplayName("close adds a feature.stopped event once, even when called twice")
    @Test
    void closeAddsAFeatureStoppedEventOnce() {
        EventNode<Event> parent = EventNode.all("test-stopped");
        FeatureNode node = FeatureNode.attach(parent, "sit", 500, this.test.telemetry());

        this.test.telemetry().inSpan("shutdown", Attributes.empty(), () -> {
            node.close();
            node.close();
        });

        List<EventData> stopped = this.test.span("shutdown").getEvents().stream().filter(event -> event.getName().equals("feature.stopped")).toList();
        Assertions.assertEquals(1, stopped.size(), "a second close must not add another event");
        Assertions.assertEquals("sit", stopped.getFirst().getAttributes().get(Telemetry.FEATURE));
    }

    @DisplayName("attach and close outside any span do nothing and do not throw")
    @Test
    void attachAndCloseWithoutACurrentSpanAreHarmless() {
        EventNode<Event> parent = EventNode.all("test-no-span");

        Assertions.assertDoesNotThrow(() -> FeatureNode.attach(parent, "sit", 500, this.test.telemetry()).close());

        Assertions.assertTrue(this.test.spans().isEmpty());
    }
}
