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
package net.onelitefeather.titan.app.testutils;

import java.util.UUID;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.testing.Env;

/**
 * A fresh, uniquely named {@code titan} test node, attached under {@code env}'s global event
 * handler - exactly the shape {@code app.bootstrap.PlatformBeans} attaches in production, minus the
 * {@code BeanScope}.
 *
 * <p>A test builds one feature (or several, sharing the same node, for a cross-feature test)
 * directly against {@link #node()}, then closes this fixture - ideally via try-with-resources -
 * once done, so the node never leaks into a later test (F.I.R.S.T. - Independent).
 */
public final class TestTitanNode implements AutoCloseable {

    private final EventNode<Event> global;
    private final EventNode<Event> node;

    private TestTitanNode(EventNode<Event> global, EventNode<Event> node) {
        this.global = global;
        this.node = node;
    }

    /**
     * @param env the Microtus/Cyano test environment whose global event handler the new node
     *            attaches under
     * @return a fresh node, already attached
     */
    public static TestTitanNode attach(Env env) {
        EventNode<Event> global = env.process().eventHandler();
        EventNode<Event> node = EventNode.all("test-titan-" + UUID.randomUUID());
        global.addChild(node);
        return new TestTitanNode(global, node);
    }

    /** @return this fixture's own event node, ready for a feature to attach its own child onto */
    public EventNode<Event> node() {
        return this.node;
    }

    /** Detaches {@link #node()} from the global event handler it was attached under. */
    @Override
    public void close() {
        this.global.removeChild(this.node);
    }
}
