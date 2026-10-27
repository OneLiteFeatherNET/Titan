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
package net.onelitefeather.titan.platform.otis;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import net.onelitefeather.titan.core.settings.SettingKey;
import net.onelitefeather.titan.core.settings.SettingValue;
import net.onelitefeather.titan.core.settings.SettingsStoreException;
import net.onelitefeather.titan.core.settings.SettingsTelemetry;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** A token that Otis rejects must not show up in any log line or failure message. */
class SettingsLogRedactionTest {

    private static final String TOKEN = "very-secret-otis-token";
    private static final UUID PLAYER = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final SettingKey TAG = SettingKey.parse("titan:tag.enabled");

    @DisplayName("A rejected token appears in neither the log nor the failure message")
    @Test
    void rejectedTokenIsNeverLogged() throws IOException {
        Logger logger = (Logger) LoggerFactory.getLogger(OtisSettingsStore.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        try (FakeOtis otis = new FakeOtis().always(FakeOtis.Reply.empty(401))) {
            OtisSettings settings = new OtisSettings(otis.url(), TOKEN, Duration.ofSeconds(1), Duration.ofSeconds(2), 0, 1, Duration.ofSeconds(30));
            OtisSettingsStore store = OtisSettingsStore.create(settings, Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC), new SettingsTelemetry(Telemetry.noop()));
            SettingsStoreException failure = Assertions.assertThrows(SettingsStoreException.class, () -> store.save(PLAYER, TAG, new SettingValue.Flag(true)), "a 401 must fail the save");
            Assertions.assertFalse(failure.getMessage().contains(TOKEN), "the failure message must not contain the token");
            // The one-failure threshold opened the breaker, so the warning about it was logged too.
            Assertions.assertFalse(messages(appender).isEmpty(), "the open breaker must have logged a warning to check");
            for (String message : messages(appender)) {
                Assertions.assertFalse(message.contains(TOKEN), "a log line must not contain the token: " + message);
            }
        } finally {
            logger.detachAppender(appender);
        }
    }

    private static List<String> messages(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }
}
