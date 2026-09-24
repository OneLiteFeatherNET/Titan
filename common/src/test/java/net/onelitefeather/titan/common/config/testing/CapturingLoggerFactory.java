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
package net.onelitefeather.titan.common.config.testing;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Backs every {@link Logger} handed out through the {@link CapturingSLF4JServiceProvider} SPI
 * registration (see {@code META-INF/services/org.slf4j.spi.SLF4JServiceProvider} in the test
 * resources) with the same in-memory list of formatted log lines, so a test can assert that a
 * particular warning was actually logged.
 */
public final class CapturingLoggerFactory implements ILoggerFactory {

    private static final List<String> MESSAGES = new CopyOnWriteArrayList<>();

    @Override
    public Logger getLogger(String name) {
        return new CapturingLogger(name);
    }

    static void record(Level level, String loggerName, String formattedMessage) {
        MESSAGES.add(level + " " + loggerName + " - " + formattedMessage);
    }

    /**
     * All log lines recorded so far, in order, formatted as {@code "<LEVEL> <logger> - <message>"}.
     */
    public static List<String> messages() {
        return List.copyOf(MESSAGES);
    }

    /**
     * Clears recorded log lines. Tests should call this before triggering the code under test so
     * earlier tests' log lines do not leak into their assertions.
     */
    public static void clear() {
        MESSAGES.clear();
    }
}
