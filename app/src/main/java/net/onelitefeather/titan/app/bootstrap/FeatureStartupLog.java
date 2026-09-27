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
package net.onelitefeather.titan.app.bootstrap;

import java.util.Comparator;
import java.util.List;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.onelitefeather.titan.app.module.FeatureNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the single lifecycle line {@code Titan} emits once every lobby feature has started.
 *
 * <p>Pulled out on its own, mirroring the old {@code ModuleStartupLog} this replaces (see {@code
 * openspec/changes/dissolve-module-platform/tasks.md}, review finding 2), so the log line can be
 * unit-tested with a captured appender without booting a Minestom server.
 *
 * <p>There is no maintained list of feature ids to read any more (see {@code
 * openspec/changes/dissolve-module-platform/design.md}, decision 1): every feature's own {@link
 * FeatureNode#attach} names its child node {@code titan/<featureId>} and gives it {@code
 * EVENT_PRIORITY}, so {@link #startedInEventOrder(EventNode)} derives the ids straight from the
 * shared {@code titan} node's own children instead - a platform class stays in sync with whichever
 * features actually attached, without knowing any of them by name.
 */
public final class FeatureStartupLog {

    private static final String CHILD_NAME_PREFIX = FeatureNode.TITAN_NODE + "/";

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureStartupLog.class);

    private FeatureStartupLog() {
    }

    /**
     * Logs every feature currently attached to {@code titan}, in the order they process a shared
     * event (ascending {@link EventNode#getPriority()}), as a single parameterised INFO line.
     *
     * @param titan the shared event node every feature's own node attaches under
     */
    public static void startedInEventOrder(EventNode<Event> titan) {
        LOGGER.info("Lobby features started in event order: {}", idsInEventOrder(titan));
    }

    /**
     * @param titan the shared event node every feature's own node attaches under
     * @return every attached feature's id - the {@code titan/<featureId>} child node's name with
     *         the {@code titan/} prefix stripped - ordered by {@link EventNode#getPriority()}
     *         ascending
     */
    static List<String> idsInEventOrder(EventNode<Event> titan) {
        return titan.getChildren().stream().sorted(Comparator.comparingInt(EventNode::getPriority)).map(child -> child.getName().substring(CHILD_NAME_PREFIX.length())).toList();
    }
}
