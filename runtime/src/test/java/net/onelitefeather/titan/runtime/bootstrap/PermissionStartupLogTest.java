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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.onelitefeather.titan.runtime.permission.DenyAllPermissionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Unit coverage for
 * {@link PermissionStartupLog#activeService(net.onelitefeather.titan.core.permission.PermissionService)}
 * - the single INFO line {@code Titan}'s constructor logs once the active permission service is
 * resolved from the {@code BeanScope}.
 */
class PermissionStartupLogTest {

    @DisplayName("Logs exactly one parameterised INFO line naming the fallback service deny-all")
    @Test
    void logsOneInfoLineNamingTheFallbackService() {
        Logger logger = (Logger) LoggerFactory.getLogger(PermissionStartupLog.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            PermissionStartupLog.activeService(new DenyAllPermissionService());
        } finally {
            logger.detachAppender(appender);
        }

        Assertions.assertEquals(1, appender.list.size(), "must log exactly one line");
        ILoggingEvent event = appender.list.get(0);
        Assertions.assertEquals("Permissions resolved by {}", event.getMessage(), "must be the parameterised template, not a pre-built string");
        Assertions.assertEquals(1, event.getArgumentArray().length, "must carry exactly one argument: the active service's name");
        Assertions.assertEquals("deny-all", event.getArgumentArray()[0], "the argument must be the service's own name()");
        Assertions.assertEquals(ch.qos.logback.classic.Level.INFO, event.getLevel());
    }
}
