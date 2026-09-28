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
package net.onelitefeather.titan.app.module;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.ArrayList;
import java.util.List;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.observability.TitanObservability;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

/**
 * Every test builds its own parent {@link EventNode} and closes the {@link FeatureNode} in a
 * {@code finally}/try-with-resources block, so no state leaks between tests (F.I.R.S.T. -
 * Independent).
 */
@ExtendWith(MicrotusExtension.class)
class FeatureNodeTest {

    private record TestEvent() implements Event {
    }

    /** A minimal cancellable event, for exercising {@link FeatureNode#onIncludingCancelled}. */
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

    @DisplayName("attach() adds a child node with the given priority")
    @Test
    void attachAddsAChildWithTheGivenPriority() {
        EventNode<Event> parent = EventNode.all("test-attach");

        try (FeatureNode node = FeatureNode.attach(parent, "tickle", 600)) {
            boolean attached = parent.getChildren().stream().anyMatch(child -> "titan/tickle".equals(child.getName()) && child.getPriority() == 600);
            Assertions.assertTrue(attached, "the parent must have a child named titan/tickle with priority 600");
        }
    }

    @DisplayName("on() delivers the event to the listener")
    @Test
    void onDeliversTheEventToTheListener() {
        EventNode<Event> parent = EventNode.all("test-on");
        List<TestEvent> received = new ArrayList<>();

        try (FeatureNode node = FeatureNode.attach(parent, "sit", 500)) {
            node.on(TestEvent.class, received::add);
            parent.call(new TestEvent());
        }

        Assertions.assertEquals(1, received.size(), "the registered listener must receive the event");
    }

    @DisplayName("close() detaches the node so it no longer receives events")
    @Test
    void closeDetachesTheNodeSoItNoLongerReceivesEvents() {
        EventNode<Event> parent = EventNode.all("test-close");
        FeatureNode node = FeatureNode.attach(parent, "sit", 500);
        List<TestEvent> received = new ArrayList<>();
        node.on(TestEvent.class, received::add);
        parent.call(new TestEvent());
        Assertions.assertEquals(1, received.size(), "the listener must fire before close()");

        node.close();
        parent.call(new TestEvent());

        Assertions.assertEquals(1, received.size(), "no further event may reach the feature once its node is closed");
    }

    @DisplayName("close() is idempotent")
    @Test
    void closeIsIdempotent() {
        EventNode<Event> parent = EventNode.all("test-close-idempotent");
        FeatureNode node = FeatureNode.attach(parent, "sit", 500);

        node.close();

        Assertions.assertDoesNotThrow(node::close, "closing an already-closed node must not throw");
    }

    @DisplayName("on() skips a listener once the event is already cancelled")
    @Test
    void onSkipsAnAlreadyCancelledEvent() {
        EventNode<Event> parent = EventNode.all("test-on-skip-cancelled");
        List<TestCancellableEvent> received = new ArrayList<>();

        try (FeatureNode node = FeatureNode.attach(parent, "protection", 100)) {
            node.on(TestCancellableEvent.class, received::add);
            TestCancellableEvent event = new TestCancellableEvent();
            event.setCancelled(true);

            parent.call(event);
        }

        Assertions.assertTrue(received.isEmpty(), "on() must not deliver an event that is already cancelled");
    }

    @DisplayName("onIncludingCancelled() still delivers an already cancelled event")
    @Test
    void onIncludingCancelledDeliversAnAlreadyCancelledEvent() {
        EventNode<Event> parent = EventNode.all("test-including-cancelled");
        List<TestCancellableEvent> received = new ArrayList<>();

        try (FeatureNode node = FeatureNode.attach(parent, "navigator", 400)) {
            node.onIncludingCancelled(TestCancellableEvent.class, received::add);
            TestCancellableEvent event = new TestCancellableEvent();
            event.setCancelled(true);

            parent.call(event);

            Assertions.assertEquals(List.of(event), received, "onIncludingCancelled() must still deliver an already cancelled event");
        }
    }

    @DisplayName("A listener registered via on() that throws is caught, and the report names the feature and the player")
    @Test
    void aThrowingListenerIsCaughtAndAttributedToFeatureAndPlayer(Env env) {
        TitanObservability.installExceptionHandler();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        Logger logger = (Logger) LoggerFactory.getLogger(TitanObservability.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        EventNode<Event> parent = EventNode.all("test-attribution");
        FeatureNode node = FeatureNode.attach(parent, "tickle", 600);
        node.on(PlayerTestEvent.class, event -> {
            throw new IllegalStateException("boom for " + event.getPlayer().getUsername());
        });

        try {
            Assertions.assertDoesNotThrow(() -> parent.call(new PlayerTestEvent(player)), "a failing listener must not propagate out of dispatch - the lobby keeps running");
        } finally {
            node.close();
            logger.detachAppender(appender);
        }

        Assertions.assertEquals(1, appender.list.size(), "exactly one failure must be reported");
        String message = appender.list.get(0).getFormattedMessage();
        Assertions.assertTrue(message.contains("tickle"), "the report must name the feature: " + message);
        Assertions.assertTrue(message.contains(player.getUsername()), "the report must name the player: " + message);
    }
}
