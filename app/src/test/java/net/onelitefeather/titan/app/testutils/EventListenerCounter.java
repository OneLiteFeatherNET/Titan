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

import net.minestom.server.event.EventNode;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

/**
 * Test-only helper that counts how many {@link net.minestom.server.event.EventListener}s are
 * registered directly on a given {@link EventNode}.
 *
 * <p>Minestom offers no public API to read this back, so this reflects into
 * {@code EventNodeImpl#listenerMap} - an implementation detail that may need adjusting on a
 * Minestom upgrade.
 */
public final class EventListenerCounter {

    private static final Field LISTENER_MAP_FIELD;

    static {
        try {
            Class<?> eventNodeImplClass = Class.forName("net.minestom.server.event.EventNodeImpl");
            Field field = eventNodeImplClass.getDeclaredField("listenerMap");
            field.setAccessible(true);
            LISTENER_MAP_FIELD = field;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private EventListenerCounter() {
    }

    /** Counts listeners registered directly on {@code node} only, not on parent or child nodes. */
    public static int countListeners(EventNode<?> node) {
        try {
            Map<?, ?> listenerMap = (Map<?, ?>) LISTENER_MAP_FIELD.get(node);
            int total = 0;
            for (Object listenerEntry : listenerMap.values()) {
                total += countListenersInEntry(listenerEntry);
            }
            return total;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to count listeners on " + node + " via reflection", exception);
        }
    }

    private static int countListenersInEntry(Object listenerEntry) throws ReflectiveOperationException {
        Field listenersField = listenerEntry.getClass().getDeclaredField("listeners");
        listenersField.setAccessible(true);
        Collection<?> listeners = (Collection<?>) listenersField.get(listenerEntry);
        return listeners.size();
    }
}
