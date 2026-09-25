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
import io.avaje.config.Configuration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Unit coverage for {@link ConfigurationStartupLog#activeProfiles(Configuration)} - the single
 * INFO line {@code net.onelitefeather.titan.app.Titan}'s constructor logs once its {@link
 * Configuration} bean has been built (see {@code
 * openspec/changes/standardized-config-profiles/design.md}, decision 6),
 * pulled out on its own so this can be asserted without touching the filesystem or the real
 * process environment. Builds its own {@link ListAppender} and detaches it in a {@code finally},
 * per test (F.I.R.S.T. - Independent), rather than sharing one across tests - the same pattern
 * {@link ModuleStartupLogTest} uses. Every {@link Configuration} here is built directly from a
 * {@link Map} (design.md decision 1's spike result), never from the real environment's {@code
 * AVAJE_PROFILES}, so the test result never depends on what happens to be set outside the test
 * itself (F.I.R.S.T. - Repeatable).
 */
class ConfigurationStartupLogTest {

    @DisplayName("With no active profile, logs one INFO line naming an empty list")
    @Test
    void logsOneInfoLineWithAnEmptyListWhenNoProfileIsActive() {
        Configuration configuration = Configuration.builder().putAll(Map.of()).build();
        Logger logger = (Logger) LoggerFactory.getLogger(ConfigurationStartupLog.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            ConfigurationStartupLog.activeProfiles(configuration);
        } finally {
            logger.detachAppender(appender);
        }

        Assertions.assertEquals(1, appender.list.size(), "must log exactly one line");
        ILoggingEvent event = appender.list.get(0);
        Assertions.assertEquals("Active configuration profiles: {}", event.getMessage(), "must be the parameterised template, not a pre-built string");
        Assertions.assertArrayEquals(new Object[]{List.of()}, event.getArgumentArray(), "with no active profile, the argument must be an empty list");
        Assertions.assertEquals(ch.qos.logback.classic.Level.INFO, event.getLevel());
    }

    @DisplayName("With active profiles set, logs one INFO line naming every active profile")
    @Test
    void logsOneInfoLineNamingEveryActiveProfile() {
        Configuration configuration = Configuration.builder().putAll(Map.of("avaje.profiles", "dev,secondary")).build();
        Logger logger = (Logger) LoggerFactory.getLogger(ConfigurationStartupLog.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            ConfigurationStartupLog.activeProfiles(configuration);
        } finally {
            logger.detachAppender(appender);
        }

        Assertions.assertEquals(1, appender.list.size(), "must log exactly one line");
        ILoggingEvent event = appender.list.get(0);
        Assertions.assertArrayEquals(new Object[]{List.of("dev", "secondary")}, event.getArgumentArray(), "must carry every active profile, in order, as the single argument");
    }
}
