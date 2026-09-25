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
package net.onelitefeather.titan.setup;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Unit coverage for {@link TitanLauncher#startCleanly(Runnable)} - the seam
 * {@link TitanLauncher#main} runs {@link Titan#instance()} through, so a broken {@code
 * application.yaml} (surfacing as {@link ExceptionInInitializerError} from the static
 * {@code io.avaje.config.Config} facade's own first touch, inside {@link Titan}'s constructor)
 * aborts startup cleanly instead of leaving the process half-started, the same way
 * {@code net.onelitefeather.titan.app.TitanApplication#main} already does for the lobby.
 *
 * <p>Hermetic: no real {@code application.yaml}, working directory or {@code MinecraftServer} is
 * touched. A fabricated {@link ExceptionInInitializerError} - with a {@code null} message, exactly
 * like the real facade throws it - stands in for a genuinely broken file. What is asserted here,
 * hermetically, is that {@link TitanLauncher#startCleanly(Runnable)} catches it, logs exactly once
 * at ERROR with a header that is not the literal text {@code "null"}, keeps the throwable (and
 * with it the full cause chain - file and line/column) attached to the log event, and reports
 * failure instead of letting it propagate (F.I.R.S.T. - Independent/Repeatable: a fresh
 * {@link ListAppender} per test, detached in a {@code finally}, mirroring
 * {@code ConfigurationStartupLogTest}).
 */
class TitanLauncherTest {

    @DisplayName("A startup step that completes normally reports success and logs nothing")
    @Test
    void successfulStartupReportsTrueAndLogsNothing() {
        Logger logger = (Logger) LoggerFactory.getLogger(TitanLauncher.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        boolean succeeded;
        try {
            succeeded = TitanLauncher.startCleanly(() -> {
            });
        } finally {
            logger.detachAppender(appender);
        }

        Assertions.assertTrue(succeeded, "a startup step that does not throw must be reported as successful");
        Assertions.assertTrue(appender.list.isEmpty(), "nothing must be logged when startup succeeds");
    }

    @DisplayName("A broken application.yaml's ExceptionInInitializerError is caught, logged once at ERROR with a readable header, and reported as a failure")
    @Test
    void exceptionInInitializerErrorFromStartupAbortsCleanly() {
        // Mirrors the real io.avaje.config.Config facade: a broken application.yaml fails its
        // static initializer with a null-message ExceptionInInitializerError, whose cause chain
        // (not its own message) names the file and the line/column.
        RuntimeException parserFailure = new IllegalStateException("Error loading properties - application.yaml", new RuntimeException("mapping values are not allowed here in 'reader', line 3, column 13"));
        ExceptionInInitializerError brokenConfig = new ExceptionInInitializerError(parserFailure);
        Logger logger = (Logger) LoggerFactory.getLogger(TitanLauncher.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        boolean succeeded;
        try {
            succeeded = TitanLauncher.startCleanly(() -> {
                throw brokenConfig;
            });
        } finally {
            logger.detachAppender(appender);
        }

        Assertions.assertFalse(succeeded, "an ExceptionInInitializerError from the startup step must be reported as a failure, not rethrown");
        Assertions.assertEquals(1, appender.list.size(), "must log exactly one line, never silently swallow the failure");
        ILoggingEvent event = appender.list.get(0);
        Assertions.assertEquals(Level.ERROR, event.getLevel(), "a startup failure must be logged at ERROR");
        Assertions.assertFalse(event.getFormattedMessage().contains("null"), "the header must stay readable, not \"Titan setup failed to start: null\", even though ExceptionInInitializerError#getMessage() is null");
        Assertions.assertInstanceOf(LoggingEvent.class, event, "logback's own event implementation is expected here, to reach the attached throwable");
        ThrowableProxy throwableProxy = (ThrowableProxy) ((LoggingEvent) event).getThrowableProxy();
        Assertions.assertSame(brokenConfig, throwableProxy.getThrowable(), "the original Error must be attached to the log event, so its full cause chain (file + line/column) reaches the ERROR log/Sentry");
    }
}
