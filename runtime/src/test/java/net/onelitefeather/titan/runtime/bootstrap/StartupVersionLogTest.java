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
package net.onelitefeather.titan.runtime.bootstrap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.onelitefeather.titan.common.observability.TitanBuild;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Unit coverage for {@link StartupVersionLog}: the single INFO line that names the running build.
 */
class StartupVersionLogTest {

    @DisplayName("Logs one INFO line with the version and commit as arguments")
    @Test
    void logsOneInfoLineWithVersionAndCommit() {
        Logger logger = (Logger) LoggerFactory.getLogger(StartupVersionLog.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            StartupVersionLog.log(new TitanBuild("3.2.1", "99a7f62"));
        } finally {
            logger.detachAppender(appender);
        }

        Assertions.assertEquals(1, appender.list.size(), "must log exactly one line");
        ILoggingEvent event = appender.list.get(0);
        Assertions.assertEquals(Level.INFO, event.getLevel(), "the startup line must be INFO");
        Assertions.assertEquals("Titan {} (commit {}) starting", event.getMessage(), "must be the parameterised template, so Loki sees one stable message");
        Assertions.assertArrayEquals(new Object[]{"3.2.1", "99a7f62"}, event.getArgumentArray(), "the version and commit must be the arguments, in that order");
    }
}
