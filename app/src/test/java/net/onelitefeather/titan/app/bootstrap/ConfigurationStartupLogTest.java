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
 * Unit coverage for {@link ConfigurationStartupLog#activeProfiles()} - the single INFO line
 * {@code net.onelitefeather.titan.app.Titan}'s constructor logs once the {@code
 * io.avaje.config.Config} facade has been initialised (see {@code
 * openspec/changes/standardized-config-profiles/design.md}, decision 6), pulled out on its own so
 * this can be asserted with a captured appender. Builds its own {@link ListAppender} and detaches
 * it in a {@code finally}, per test (F.I.R.S.T. - Independent), the same pattern
 * {@link ModuleStartupLogTest} uses.
 *
 * <p>Since {@code openspec/changes/avaje-config-facade/design.md} decision 1,
 * {@link ConfigurationStartupLog#activeProfiles()} reads the static {@code Config} facade itself
 * instead of taking an injected {@code Configuration}, so this test now shares that facade's
 * one-time, JVM-wide initialisation with every other test in this process - the shipped
 * classpath {@code application.yaml}, with no test-only override (design.md decision 5: no
 * {@code application-test.yaml}, and no test may call a {@code Config} mutator). This only asserts
 * the shape of the line and that the argument is the list avaje-config actually resolved, not a
 * fabricated one; the "a profile changes the value" scenario is covered by the child-JVM
 * {@code ConfigurationPrecedenceTest} instead (design.md decision 5).
 */
class ConfigurationStartupLogTest {

    @DisplayName("Logs exactly one parameterised INFO line naming the active configuration profiles")
    @Test
    void logsOneInfoLineNamingTheActiveProfiles() {
        Logger logger = (Logger) LoggerFactory.getLogger(ConfigurationStartupLog.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            ConfigurationStartupLog.activeProfiles();
        } finally {
            logger.detachAppender(appender);
        }

        Assertions.assertEquals(1, appender.list.size(), "must log exactly one line");
        ILoggingEvent event = appender.list.get(0);
        Assertions.assertEquals("Active configuration profiles: {}", event.getMessage(), "must be the parameterised template, not a pre-built string");
        Assertions.assertEquals(1, event.getArgumentArray().length, "must carry exactly one argument: the list of active profiles");
        Assertions.assertInstanceOf(List.class, event.getArgumentArray()[0], "the argument must be the list avaje-config resolved, not a formatted string");
        Assertions.assertEquals(ch.qos.logback.classic.Level.INFO, event.getLevel());
    }
}
