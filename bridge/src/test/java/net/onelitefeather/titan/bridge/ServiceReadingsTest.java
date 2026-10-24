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
package net.onelitefeather.titan.bridge;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceReadingsTest {

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(ServiceReadings.class);

    @BeforeEach
    void attachAppender() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        appender.stop();
    }

    private List<Level> levels() {
        return appender.list.stream().map(ILoggingEvent::getLevel).toList();
    }

    @DisplayName("A missing provider yields no readings and one warning however often it is asked")
    @Test
    void warnsOnceWhenProviderMissing() {
        ServiceReadings readings = new ServiceReadings(() -> null);

        assertTrue(readings.read("task", "Lobby").isEmpty(), "no provider means no readings");
        readings.read("task", "Lobby");
        readings.read("group", "Lobby");

        assertEquals(List.of(Level.WARN), levels(), "exactly one warning");
    }

    @DisplayName("A resolved provider is read and announced once")
    @Test
    void logsInfoOnceWhenResolved() {
        ServiceReading reading = new ServiceReading("Lobby-1", true, 2, 10);
        ServiceReadings readings = new ServiceReadings(() -> (type, name) -> List.of(reading));

        assertEquals(List.of(reading), readings.read("task", "Lobby"), "reading of the provider");
        readings.read("task", "Lobby");

        assertEquals(List.of(Level.INFO), levels(), "exactly one info");
    }

    @DisplayName("A provider that appears later is picked up")
    @Test
    void retriesUntilProviderAvailable() {
        List<ServiceReadings.Source> sources = new ArrayList<>();
        sources.add(null);
        sources.add((type, name) -> List.of(new ServiceReading("Lobby-1", true, 1, 5)));
        ServiceReadings readings = new ServiceReadings(sources::removeFirst);

        assertTrue(readings.read("task", "Lobby").isEmpty(), "first lookup: not available");
        assertEquals(1, readings.read("task", "Lobby").size(), "second lookup: available");
        assertEquals(List.of(Level.WARN, Level.INFO), levels(), "warned, then resolved");
    }

    @DisplayName("Exceptions of the provider propagate")
    @Test
    void propagatesProviderFailures() {
        ServiceReadings readings = new ServiceReadings(() -> (type, name) -> {
            throw new IllegalStateException("boom");
        });

        assertThrows(IllegalStateException.class, () -> readings.read("task", "Lobby"), "must not be swallowed");
    }
}
