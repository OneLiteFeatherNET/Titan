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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Unit coverage for {@link ModuleStartupLog#enabledInOrder(List)} - the single INFO line
 * {@code Titan} logs once every lobby module has been enabled (see
 * {@code openspec/changes/avaje-dependency-injection/design.md}, decision 5), pulled out on its
 * own so this can be asserted without booting a Minestom server. Builds its own
 * {@link ListAppender} and detaches it in a {@code finally}, per test (F.I.R.S.T. - Independent),
 * rather than sharing one across tests.
 */
class ModuleStartupLogTest {

    @DisplayName("Logs one parameterised INFO line naming the modules in order")
    @Test
    void logsOneInfoLineWithTheModuleOrder() {
        Logger logger = (Logger) LoggerFactory.getLogger(ModuleStartupLog.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        List<String> order = List.of("protection", "spawn", "respawn", "navigator", "sit", "tickle", "elytra");
        try {
            ModuleStartupLog.enabledInOrder(order);
        } finally {
            logger.detachAppender(appender);
        }

        Assertions.assertEquals(1, appender.list.size(), "must log exactly one line");
        ILoggingEvent event = appender.list.get(0);
        Assertions.assertEquals("Lobby modules enabled in order: {}", event.getMessage(), "must be the parameterised template, not a pre-built string");
        Assertions.assertArrayEquals(new Object[]{order}, event.getArgumentArray(), "must carry the module ids, in order, as the single argument");
        Assertions.assertEquals(ch.qos.logback.classic.Level.INFO, event.getLevel());
    }
}
