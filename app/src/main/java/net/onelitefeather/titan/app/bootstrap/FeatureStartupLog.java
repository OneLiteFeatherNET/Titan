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
 * <p>Derives the ids from {@code titan}'s own child nodes (named {@code titan/<featureId>} by
 * {@link FeatureNode#attach}), so it stays in sync without knowing any feature by name.
 */
public final class FeatureStartupLog {

    private static final String CHILD_NAME_PREFIX = FeatureNode.TITAN_NODE + "/";

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureStartupLog.class);

    private FeatureStartupLog() {
    }

    /**
     * Logs every attached feature, in ascending {@link EventNode#getPriority()} order, as one INFO
     * line.
     */
    public static void startedInEventOrder(EventNode<Event> titan) {
        LOGGER.info("Lobby features started in event order: {}", idsInEventOrder(titan));
    }

    static List<String> idsInEventOrder(EventNode<Event> titan) {
        return titan.getChildren().stream().sorted(Comparator.comparingInt(EventNode::getPriority)).map(child -> child.getName().substring(CHILD_NAME_PREFIX.length())).toList();
    }
}
