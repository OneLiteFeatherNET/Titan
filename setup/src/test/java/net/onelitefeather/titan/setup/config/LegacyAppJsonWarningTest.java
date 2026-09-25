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
package net.onelitefeather.titan.setup.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link LegacyAppJsonWarning#warnIfLegacyAppJsonPresent(Path)}: the setup server does not
 * migrate {@code app.json} itself, so an operator who deploys it before the lobby's next start
 * gets one WARN naming that {@code spawn.simulationDistance} still uses its default (see {@link
 * SetupSpawnConfig#DEFAULTS}) until the lobby migrates the file. Builds its own {@link
 * ListAppender} and detaches it in a {@code finally}, per test (F.I.R.S.T. - Independent), the same
 * pattern {@code app}'s {@code ConfigurationStartupLogTest} uses.
 */
class LegacyAppJsonWarningTest {

    @DisplayName("app.json present, application.yaml absent: logs one WARN and writes nothing")
    @Test
    void appJsonWithoutApplicationYamlLogsOneWarning(@TempDir Path workingDir) throws IOException {
        Files.writeString(workingDir.resolve("app.json"), "{}");
        ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            LegacyAppJsonWarning.warnIfLegacyAppJsonPresent(workingDir);
        } finally {
            detachAppender(appender);
        }

        assertEquals(1, appender.list.size(), "must log exactly one line");
        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.WARN, event.getLevel());
        String message = event.getFormattedMessage();
        assertTrue(message.contains("app.json"), "message should name app.json: " + message);
        assertTrue(message.toLowerCase().contains("lobby"), "message should say the lobby migrates it: " + message);
        assertTrue(message.contains("spawn.simulationDistance"), "message should name the affected key: " + message);
        try (var entries = Files.list(workingDir)) {
            assertEquals(1, entries.count(), "the setup server must write nothing - only the original app.json may exist");
        }
    }

    @DisplayName("Both app.json and application.yaml present: logs nothing")
    @Test
    void bothFilesPresentLogsNothing(@TempDir Path workingDir) throws IOException {
        Files.writeString(workingDir.resolve("app.json"), "{}");
        Files.writeString(workingDir.resolve("application.yaml"), "spawn:\n  simulationDistance: 4\n");
        ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            LegacyAppJsonWarning.warnIfLegacyAppJsonPresent(workingDir);
        } finally {
            detachAppender(appender);
        }

        assertEquals(0, appender.list.size(), "must not warn when application.yaml already exists");
    }

    @DisplayName("Only application.yaml present: logs nothing")
    @Test
    void onlyApplicationYamlPresentLogsNothing(@TempDir Path workingDir) throws IOException {
        Files.writeString(workingDir.resolve("application.yaml"), "spawn:\n  simulationDistance: 4\n");
        ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            LegacyAppJsonWarning.warnIfLegacyAppJsonPresent(workingDir);
        } finally {
            detachAppender(appender);
        }

        assertEquals(0, appender.list.size(), "must not warn when there is no app.json to worry about");
    }

    @DisplayName("Neither file present: logs nothing")
    @Test
    void neitherFilePresentLogsNothing(@TempDir Path workingDir) {
        ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            LegacyAppJsonWarning.warnIfLegacyAppJsonPresent(workingDir);
        } finally {
            detachAppender(appender);
        }

        assertEquals(0, appender.list.size(), "must not warn on a directory with neither file");
    }

    private static ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(LegacyAppJsonWarning.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detachAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(LegacyAppJsonWarning.class);
        logger.detachAppender(appender);
    }
}
