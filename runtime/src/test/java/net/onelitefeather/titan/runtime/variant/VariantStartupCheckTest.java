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
package net.onelitefeather.titan.runtime.variant;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** Unit coverage for {@link VariantStartupCheck#verify(VariantDescriptor, List)}. */
class VariantStartupCheckTest {

    @DisplayName("Every expected module loaded logs exactly one parameterised INFO line naming the variant and its modules")
    @Test
    void allModulesLoadedLogsOneInfoLine() {
        VariantDescriptor descriptor = VariantDescriptor.of("cloudnet", List.of("adminColumn", "sitColumn"));
        Logger logger = (Logger) LoggerFactory.getLogger(VariantStartupCheck.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            VariantStartupCheck.verify(descriptor, List.of("adminColumn", "sitColumn"));
        } finally {
            logger.detachAppender(appender);
        }

        Assertions.assertEquals(1, appender.list.size(), "must log exactly one line");
        ILoggingEvent event = appender.list.get(0);
        Assertions.assertEquals("Variant {} started with modules {}", event.getMessage(), "must be the parameterised template, not a pre-built string");
        Assertions.assertArrayEquals(new Object[]{"cloudnet", List.of("adminColumn", "sitColumn")}, event.getArgumentArray());
        Assertions.assertEquals(Level.INFO, event.getLevel());
    }

    @DisplayName("A missing module aborts with an IllegalStateException naming it")
    @Test
    void aMissingModuleAbortsNamingIt() {
        VariantDescriptor descriptor = VariantDescriptor.of("cloudnet", List.of("adminColumn", "sitColumn"));

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> VariantStartupCheck.verify(descriptor, List.of("adminColumn")));

        Assertions.assertTrue(thrown.getMessage().contains("sitColumn"), "the message must name the missing module, was: " + thrown.getMessage());
        Assertions.assertTrue(thrown.getMessage().contains("cloudnet"), "the message must name the variant, was: " + thrown.getMessage());
    }

    @DisplayName("verify(ClassLoader) skips the check entirely when no variant.properties is on the classpath")
    @Test
    void skipsWhenNoVariantPropertiesIsOnTheClasspath() {
        Assertions.assertDoesNotThrow(() -> VariantStartupCheck.verify(getClass().getClassLoader()), "runtime's own test classpath ships no variant.properties");
    }
}
