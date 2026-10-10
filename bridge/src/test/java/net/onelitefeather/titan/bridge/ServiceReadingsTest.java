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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceReadingsTest {

    private final RecordingLogger logger = new RecordingLogger();

    @DisplayName("A missing provider yields no readings and one warning however often it is asked")
    @Test
    void warnsOnceWhenProviderMissing() {
        ServiceReadings readings = new ServiceReadings(() -> null, logger);

        assertTrue(readings.read("task", "Lobby").isEmpty(), "no provider means no readings");
        readings.read("task", "Lobby");
        readings.read("group", "Lobby");

        assertEquals(List.of(Level.WARNING), logger.levels, "exactly one warning");
    }

    @DisplayName("A resolved provider is read and announced once")
    @Test
    void logsInfoOnceWhenResolved() {
        ServiceReading reading = new ServiceReading("Lobby-1", true, 2, 10);
        ServiceReadings readings = new ServiceReadings(() -> (type, name) -> List.of(reading), logger);

        assertEquals(List.of(reading), readings.read("task", "Lobby"), "reading of the provider");
        readings.read("task", "Lobby");

        assertEquals(List.of(Level.INFO), logger.levels, "exactly one info");
    }

    @DisplayName("A provider that appears later is picked up")
    @Test
    void retriesUntilProviderAvailable() {
        List<ServiceReadings.Source> sources = new ArrayList<>();
        sources.add(null);
        sources.add((type, name) -> List.of(new ServiceReading("Lobby-1", true, 1, 5)));
        ServiceReadings readings = new ServiceReadings(sources::removeFirst, logger);

        assertTrue(readings.read("task", "Lobby").isEmpty(), "first lookup: not available");
        assertEquals(1, readings.read("task", "Lobby").size(), "second lookup: available");
        assertEquals(List.of(Level.WARNING, Level.INFO), logger.levels, "warned, then resolved");
    }

    @DisplayName("Exceptions of the provider propagate")
    @Test
    void propagatesProviderFailures() {
        ServiceReadings readings = new ServiceReadings(() -> (type, name) -> {
            throw new IllegalStateException("boom");
        }, logger);

        assertThrows(IllegalStateException.class, () -> readings.read("task", "Lobby"), "must not be swallowed");
    }

    private static final class RecordingLogger implements Logger {
        private final List<Level> levels = new ArrayList<>();

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public boolean isLoggable(Level level) {
            return true;
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
            levels.add(level);
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String format, Object... params) {
            levels.add(level);
        }
    }
}
