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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
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
import net.minestom.server.event.trait.PlayerEvent;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * A feature's own event node, attached under the shared {@code titan} node: a feature calls
 * {@link #attach(EventNode, String, int)} in its own {@code @PostConstruct}, registers through
 * {@link #on(Class, Consumer)} or {@link #onIncludingCancelled(Class, Consumer)}, and disconnects
 * with {@link #close()} in its {@code @PreDestroy}. Each instance wraps its own node and holds no
 * shared or static state, so no synchronization is needed.
 *
 * <p>Every listener registered here is wrapped in {@link ListenerGuard#guard(String, Consumer)}
 * with {@code featureId}, so a failure keeps the lobby running and the report names the feature
 * and, if the event carries one, the player.
 */
public final class FeatureNode implements AutoCloseable {

    /** The {@code @Named} qualifier of the shared {@code titan} {@link EventNode} bean. */
    public static final String TITAN_NODE = "titan";

    /** Events that fire per tick, move or packet: a span each would flood the exporter. */
    private static final List<Class<? extends Event>> HIGH_FREQUENCY_EVENTS = List.of(PlayerMoveEvent.class, PlayerPacketEvent.class, PlayerPacketOutEvent.class, PlayerChunkLoadEvent.class, PlayerChunkUnloadEvent.class, PlayerTickEvent.class, PlayerTickEndEvent.class, EntityTickEvent.class, InstanceTickEvent.class, ServerTickMonitorEvent.class);

    private final EventNode<Event> parent;
    private final EventNode<Event> node;
    private final String featureId;
    private final Telemetry telemetry;
    private final AtomicBoolean closed = new AtomicBoolean();

    private FeatureNode(EventNode<Event> parent, EventNode<Event> node, String featureId, Telemetry telemetry) {
        this.parent = parent;
        this.node = node;
        this.featureId = featureId;
        this.telemetry = telemetry;
    }

    /**
     * Creates {@code featureId}'s own event node, named {@code titan/<featureId>}, and attaches
     * it to {@code parent} immediately at the given priority.
     *
     * <p>{@code priority} becomes the node's {@link EventNode#setPriority(int)}, which decides the
     * execution order among sibling feature nodes reacting to the same event.
     *
     * @throws IllegalStateException if {@code parent} already has a child at {@code priority} -
     *                               naming both feature ids and the position, since the lobby must
     *                               not start with an
     *                               undocumented tie-break between them
     */
    public static FeatureNode attach(EventNode<Event> parent, String featureId, int priority) {
        return attach(parent, featureId, priority, Telemetry.noop());
    }

    /**
     * Like {@link #attach(EventNode, String, int)}, with the {@link Telemetry} this feature's
     * listeners count failures and open spans with. Adds a {@code feature.started} event to the
     * current span, which is a no-op when there is none.
     */
    public static FeatureNode attach(EventNode<Event> parent, String featureId, int priority, Telemetry telemetry) {
        parent.getChildren().stream().filter(child -> child.getPriority() == priority).findFirst().ifPresent(colliding -> {
            throw new IllegalStateException("Features '" + featureId + "' and '" + colliding.getName().replace("titan/", "") + "' both use event priority " + priority);
        });
        EventNode<Event> node = EventNode.all("titan/" + featureId);
        node.setPriority(priority);
        parent.addChild(node);
        Span.current().addEvent("feature.started", featureAttributes(featureId).toBuilder().put(Telemetry.FEATURE_PRIORITY, (long) priority).build());
        return new FeatureNode(parent, node, featureId, telemetry);
    }

    private static Attributes featureAttributes(String featureId) {
        return Attributes.of(Telemetry.FEATURE, featureId);
    }

    /**
     * Registers {@code listener} for {@code type}, guarded so a failure is attributed to this
     * feature without stopping the lobby.
     *
     * <p>Skipped once a {@link net.minestom.server.event.trait.CancellableEvent} is already
     * cancelled by the time it reaches this node; use {@link #onIncludingCancelled} to react
     * anyway.
     */
    public <E extends Event> FeatureNode on(Class<E> type, Consumer<E> listener) {
        this.node.addListener(type, ListenerGuard.guard(this.featureId, this.telemetry, listener));
        return this;
    }

    /**
     * Registers {@code listener} for {@code type}, like {@link #on}, but runs even if the event is
     * already cancelled.
     */
    public <E extends Event> FeatureNode onIncludingCancelled(Class<E> type, Consumer<E> listener) {
        this.node.addListener(EventListener.builder(type).ignoreCancelled(false).handler(ListenerGuard.guard(this.featureId, this.telemetry, listener)).build());
        return this;
    }

    /**
     * Like {@link #on}, but the listener runs in a span named {@code spanName} carrying
     * {@code titan.feature} and, for a {@link PlayerEvent}, the player's {@code user.id}. An
     * exception is recorded on the span and then reaches the guard as usual.
     *
     * @throws IllegalArgumentException for a high-frequency event type (move, packet, chunk,
     *                                  tick); count those with a meter instead
     */
    public <E extends Event> FeatureNode onTraced(Class<E> type, String spanName, Consumer<E> listener) {
        requireLowFrequency(type);
        this.node.addListener(type, ListenerGuard.guard(this.featureId, this.telemetry, traced(spanName, listener)));
        return this;
    }

    /** Like {@link #onTraced}, but runs even if the event is already cancelled. */
    public <E extends Event> FeatureNode onTracedIncludingCancelled(Class<E> type, String spanName, Consumer<E> listener) {
        requireLowFrequency(type);
        this.node.addListener(EventListener.builder(type).ignoreCancelled(false).handler(ListenerGuard.guard(this.featureId, this.telemetry, traced(spanName, listener))).build());
        return this;
    }

    private <E extends Event> Consumer<E> traced(String spanName, Consumer<E> listener) {
        return event -> {
            Attributes attributes = featureAttributes(this.featureId);
            if (event instanceof PlayerEvent playerEvent) {
                attributes = attributes.toBuilder().put(Telemetry.USER_ID, playerEvent.getPlayer().getUuid().toString()).build();
            }
            this.telemetry.inSpan(spanName, attributes, () -> listener.accept(event));
        };
    }

    private static void requireLowFrequency(Class<? extends Event> type) {
        if (HIGH_FREQUENCY_EVENTS.stream().anyMatch(blocked -> blocked.isAssignableFrom(type))) {
            throw new IllegalArgumentException(type.getSimpleName() + " fires too often for a span per call; count it with a meter instead");
        }
    }

    /** Idempotent: a second call is a no-op. */
    @Override
    public void close() {
        this.parent.removeChild(this.node);
        if (this.closed.compareAndSet(false, true)) {
            Span.current().addEvent("feature.stopped", featureAttributes(this.featureId));
        }
    }

    /**
     * Minestom's exception handler entry point: routes an uncaught exception through
     * {@link ListenerGuard}, so it is attributed to the feature and player {@link #on}/
     * {@link #onIncludingCancelled} recorded. Public so {@code common}'s
     * {@code TitanObservability.installExceptionHandler()} can reference it without a cycle.
     */
    public static void reportUnhandledException(Throwable throwable) {
        ListenerGuard.handleException(throwable);
    }

    /**
     * Wraps {@code listener} with the same failure attribution {@link #on} uses, for a caller that
     * dispatches outside a node this class manages (e.g. an item-use handler keyed by identity
     * tag, not by an event type registered on a feature's own node).
     */
    public static <E extends Event> Consumer<E> guard(String featureId, Consumer<E> listener) {
        return ListenerGuard.guard(featureId, listener);
    }
}
