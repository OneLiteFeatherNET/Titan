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

import java.util.function.Consumer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.onelitefeather.titan.common.observability.TitanObservability;

/**
 * A feature's own event node, attached under the shared {@code titan} node: a feature calls
 * {@link #attach(EventNode, String, int)} in its own {@code @PostConstruct}, registers through
 * {@link #on(Class, Consumer)} or {@link #onIncludingCancelled(Class, Consumer)}, and disconnects
 * with {@link #close()} in its {@code @PreDestroy}.
 *
 * <p>Every listener registered here is wrapped in
 * {@link TitanObservability#guard(String, Consumer)} with {@code featureId}, so a failure keeps
 * the lobby running and the report names the feature and, if the event carries one, the player.
 */
public final class FeatureNode implements AutoCloseable {

    /** The {@code @Named} qualifier of the shared {@code titan} {@link EventNode} bean. */
    public static final String TITAN_NODE = "titan";

    private final EventNode<Event> parent;
    private final EventNode<Event> node;
    private final String featureId;

    private FeatureNode(EventNode<Event> parent, EventNode<Event> node, String featureId) {
        this.parent = parent;
        this.node = node;
        this.featureId = featureId;
    }

    /**
     * Creates {@code featureId}'s own event node, named {@code titan/<featureId>}, and attaches
     * it to {@code parent} immediately at the given priority.
     */
    public static FeatureNode attach(EventNode<Event> parent, String featureId, int priority) {
        EventNode<Event> node = EventNode.all("titan/" + featureId);
        node.setPriority(priority);
        parent.addChild(node);
        return new FeatureNode(parent, node, featureId);
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
        this.node.addListener(type, TitanObservability.guard(this.featureId, listener));
        return this;
    }

    /**
     * Registers {@code listener} for {@code type}, like {@link #on}, but runs even if the event is
     * already cancelled.
     */
    public <E extends Event> FeatureNode onIncludingCancelled(Class<E> type, Consumer<E> listener) {
        this.node.addListener(EventListener.builder(type).ignoreCancelled(false).handler(TitanObservability.guard(this.featureId, listener)).build());
        return this;
    }

    @Override
    public void close() {
        this.parent.removeChild(this.node);
    }
}
