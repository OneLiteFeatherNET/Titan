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
 * A feature's own event node, attached under the shared {@code titan} node.
 *
 * <p>Replaces {@code ModuleContext} as the door a feature bean uses to reach the Minestom event
 * tree (see {@code openspec/changes/dissolve-module-platform/design.md}, decision 1): a feature
 * calls {@link #attach(EventNode, String, int)} in its own {@code @PostConstruct}, registers
 * through {@link #on(Class, Consumer)} or {@link #onIncludingCancelled(Class, Consumer)}, and
 * disconnects with {@link #close()} in its {@code @PreDestroy} - before any other shutdown logic
 * runs, so no event reaches the feature while it tears itself down.
 *
 * <p>Deliberately holds no static state: every instance is a plain wrapper around the one child
 * node {@link #attach} created, and two features never share one.
 *
 * <h2>Event order</h2>
 *
 * <p>{@code priority} becomes the child node's {@link EventNode#setPriority(int)} - Minestom's
 * own mechanism for ordering sibling nodes - which is what now decides in which order two
 * features reacting to the same event run, replacing the old start-order guarantee.
 *
 * <h2>Error attribution</h2>
 *
 * <p>Every listener registered here is wrapped in
 * {@link TitanObservability#guard(String, Consumer)}
 * with {@code featureId}, exactly like {@code ModuleContext#listen} did: a failure keeps the lobby
 * running and the report names the feature and, if the event carries one, the player.
 */
public final class FeatureNode implements AutoCloseable {

    /**
     * The {@code @Named} qualifier of the shared {@code titan} {@link EventNode} bean every
     * feature's own node attaches under - {@code app.bootstrap.PlatformBeans} registers the bean
     * under this name, and any platform class that looks it up by name (such as
     * {@link net.onelitefeather.titan.app.module.item.LobbyItems LobbyItems} or {@code Titan})
     * references this constant instead of duplicating the literal.
     *
     * <p>Lives here rather than on {@code PlatformBeans} so the platform ({@code app.module}) never
     * has to import the composition root ({@code app.bootstrap}) just to name this qualifier - see
     * {@code openspec/changes/dissolve-module-platform/design.md}, decision 1, and
     * {@code ArchitectureTest#platformDoesNotDependOnCompositionRoot}.
     */
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
     * Creates {@code featureId}'s own event node, named {@code titan/<featureId>}, sets its
     * priority and attaches it to {@code parent} immediately.
     *
     * @param parent    the shared node the returned node attaches under - the {@code titan}
     *                  bean in production
     * @param featureId the owning feature's id, used for the node's name and for
     *                  {@link TitanObservability#guard(String, Consumer)} attribution
     * @param priority  this feature's position among its siblings; see
     *                  {@link EventNode#setPriority(int)}
     * @return the new node, ready for {@link #on} / {@link #onIncludingCancelled}
     */
    public static FeatureNode attach(EventNode<Event> parent, String featureId, int priority) {
        EventNode<Event> node = EventNode.all("titan/" + featureId);
        node.setPriority(priority);
        parent.addChild(node);
        return new FeatureNode(parent, node, featureId);
    }

    /**
     * Registers {@code listener} for {@code type}, guarded so a failure is attributed to this
     * feature (and, if the event carries one, its player) without stopping the lobby.
     *
     * <p>For a {@link net.minestom.server.event.trait.CancellableEvent}, {@code listener} is
     * skipped once the event is already cancelled by the time it reaches this node - Minestom's
     * usual behaviour for a {@code Consumer}-based listener. A feature that must react regardless
     * of an earlier feature's cancellation needs {@link #onIncludingCancelled} instead.
     *
     * @param type     the event type to listen for
     * @param listener the listener
     * @param <E>      the event type
     * @return this node, so registrations can be chained
     */
    public <E extends Event> FeatureNode on(Class<E> type, Consumer<E> listener) {
        this.node.addListener(type, TitanObservability.guard(this.featureId, listener));
        return this;
    }

    /**
     * Registers {@code listener} for {@code type}, exactly like {@link #on}, except the listener
     * still runs even if the event is already cancelled by the time it reaches this node.
     *
     * @param type     the event type to listen for
     * @param listener the listener
     * @param <E>      the event type
     * @return this node, so registrations can be chained
     */
    public <E extends Event> FeatureNode onIncludingCancelled(Class<E> type, Consumer<E> listener) {
        this.node.addListener(EventListener.builder(type).ignoreCancelled(false).handler(TitanObservability.guard(this.featureId, listener)).build());
        return this;
    }

    /**
     * Detaches this feature's node from its parent, so none of its listeners run again. Calling
     * this more than once is harmless - {@link EventNode#removeChild(EventNode)} is a no-op once
     * the child is already gone.
     */
    @Override
    public void close() {
        this.parent.removeChild(this.node);
    }
}
