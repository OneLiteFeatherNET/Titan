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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.FeatureNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

/**
 * Unit coverage for {@link FeatureStartupLog}, built against a fresh {@code titan} {@link
 * EventNode} with no running server - the same style {@link
 * net.onelitefeather.titan.app.module.FeatureNodeTest} uses. Every test builds its own node and
 * closes every attached {@link FeatureNode} in a try-with-resources block, and detaches its own
 * {@link ListAppender} in a {@code finally}, so no state leaks between tests (F.I.R.S.T. -
 * Independent).
 */
@ExtendWith(MicrotusExtension.class)
class FeatureStartupLogTest {

    @DisplayName("idsInEventOrder() derives every attached feature's id, ordered by ascending event priority")
    @Test
    void idsInEventOrderOrdersByAscendingPriority() {
        EventNode<Event> titan = EventNode.all(FeatureNode.TITAN_NODE);

        try (FeatureNode tickle = FeatureNode.attach(titan, "tickle", 600); FeatureNode protection = FeatureNode.attach(titan, "protection", 100); FeatureNode sit = FeatureNode.attach(titan, "sit", 500)) {
            Assertions.assertEquals(List.of("protection", "sit", "tickle"), FeatureStartupLog.idsInEventOrder(titan), "ids must be ordered by ascending EVENT_PRIORITY, not by attachment order");
        }
    }

    @DisplayName("startedInEventOrder() logs exactly one parameterised INFO line naming the features in event order")
    @Test
    void logsOneInfoLineNamingTheFeaturesInEventOrder() {
        EventNode<Event> titan = EventNode.all(FeatureNode.TITAN_NODE);
        Logger logger = (Logger) LoggerFactory.getLogger(FeatureStartupLog.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try (FeatureNode protection = FeatureNode.attach(titan, "protection", 100); FeatureNode spawn = FeatureNode.attach(titan, "spawn", 200)) {
            FeatureStartupLog.startedInEventOrder(titan);
        } finally {
            logger.detachAppender(appender);
        }

        Assertions.assertEquals(1, appender.list.size(), "must log exactly one line");
        ILoggingEvent event = appender.list.get(0);
        Assertions.assertEquals("Lobby features started in event order: {}", event.getMessage(), "must be the parameterised template, not a pre-built string");
        Assertions.assertArrayEquals(new Object[]{List.of("protection", "spawn")}, event.getArgumentArray(), "must carry the feature ids, in event order, as the single argument");
        Assertions.assertEquals(Level.INFO, event.getLevel());
    }
}
