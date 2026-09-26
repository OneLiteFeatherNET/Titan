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
package net.onelitefeather.titan.app.feature.tickle;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.avaje.config.Configuration;
import net.onelitefeather.titan.common.config.RuntimeConfigFallback;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for {@link TickleSettings#cooldownMillis(String)}: the {@code cooldownMillis}
 * parsing and validation described in the {@code lobby-module-config} spec ("Negative Dauer" and
 * "Ungültiger Override" scenarios). No {@code io.avaje.config.Config} static facade and no server
 * involved - {@link #configGetAsWrapsAFailureNamingTheKey} and
 * {@link #configGetAsKeepsTheNegativeDurationReasonAsTheCause} build their own, local
 * {@link Configuration} instance instead, exactly as {@code Config.getAs} would wrap this class's
 * own exceptions, without touching the static facade (F.I.R.S.T. - Independent/Repeatable).
 */
class TickleSettingsTest {

    @DisplayName("A zero cooldown is valid")
    @Test
    void zeroCooldownIsValid() {
        Assertions.assertEquals(0L, TickleSettings.cooldownMillis("0"));
    }

    @DisplayName("A positive cooldown is valid")
    @Test
    void positiveCooldownIsValid() {
        Assertions.assertEquals(1500L, TickleSettings.cooldownMillis("1500"));
    }

    @DisplayName("A negative cooldown is rejected")
    @Test
    void negativeCooldownIsRejected() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> TickleSettings.cooldownMillis("-5"));

        Assertions.assertTrue(thrown.getMessage().contains("-5"), "the message must keep the offending value, was: " + thrown.getMessage());
    }

    @DisplayName("A non-numeric cooldown fails to parse")
    @Test
    void nonNumericCooldownFailsToParse() {
        Assertions.assertThrows(NumberFormatException.class, () -> TickleSettings.cooldownMillis("abc"));
    }

    @DisplayName("Config.getAs wraps a non-numeric value, naming the key and keeping the raw value in the cause")
    @Test
    void configGetAsWrapsAFailureNamingTheKey() {
        Configuration configuration = Configuration.builder().put(TickleSettings.COOLDOWN_KEY, "abc").build();

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> configuration.getAs(TickleSettings.COOLDOWN_KEY, TickleSettings::cooldownMillis));

        Assertions.assertTrue(thrown.getMessage().contains(TickleSettings.COOLDOWN_KEY), "the message must name " + TickleSettings.COOLDOWN_KEY + ", was: " + thrown.getMessage());
        Assertions.assertInstanceOf(NumberFormatException.class, thrown.getCause(), "the cause must be the original parse failure");
        Assertions.assertTrue(thrown.getCause().getMessage().contains("abc"), "the cause must keep the offending raw value, was: " + thrown.getCause().getMessage());
    }

    @DisplayName("Config.getAs wraps a negative value, naming the key and keeping the reason as the cause")
    @Test
    void configGetAsKeepsTheNegativeDurationReasonAsTheCause() {
        Configuration configuration = Configuration.builder().put(TickleSettings.COOLDOWN_KEY, "-5").build();

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> configuration.getAs(TickleSettings.COOLDOWN_KEY, TickleSettings::cooldownMillis));

        Assertions.assertTrue(thrown.getMessage().contains(TickleSettings.COOLDOWN_KEY), "the message must name " + TickleSettings.COOLDOWN_KEY + ", was: " + thrown.getMessage());
        Assertions.assertInstanceOf(IllegalArgumentException.class, thrown.getCause(), "the cause must be this class's own validation failure");
        Assertions.assertTrue(thrown.getCause().getMessage().contains("-5"), "the cause must keep the offending value, was: " + thrown.getCause().getMessage());
    }

    private static RuntimeConfigFallback freshFallback() {
        return new RuntimeConfigFallback(Configuration.builder().put(TickleSettings.COOLDOWN_KEY, "4000").build());
    }

    @DisplayName("resolve passes a valid value through unchanged, without warning")
    @Test
    void resolveCooldownMillisPassesAValidValueThrough() {
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeConfigFallback.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            long result = freshFallback().resolve(TickleSettings.COOLDOWN_KEY, "1500", TickleSettings::cooldownMillis, () -> 4000L);

            Assertions.assertEquals(1500L, result, "a valid value must pass through unchanged");
            Assertions.assertTrue(appender.list.isEmpty(), "a valid value must never warn");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @DisplayName("resolve falls back to the shipped default and warns once for a negative value")
    @Test
    void resolveCooldownMillisFallsBackAndWarnsForANegativeValue() {
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeConfigFallback.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            long result = freshFallback().resolve(TickleSettings.COOLDOWN_KEY, "-5", TickleSettings::cooldownMillis, () -> 4000L);

            Assertions.assertEquals(4000L, result, "an invalid value must fall back to the shipped default");
            Assertions.assertEquals(1, appender.list.size(), "exactly one WARN must be logged for a new invalid value");
            ILoggingEvent event = appender.list.get(0);
            Assertions.assertEquals(ch.qos.logback.classic.Level.WARN, event.getLevel());
            Assertions.assertEquals(TickleSettings.COOLDOWN_KEY, event.getArgumentArray()[0], "the first argument must be the full key");
            Assertions.assertEquals("-5", event.getArgumentArray()[1], "the second argument must be the offending raw value");
            Assertions.assertEquals(4000L, event.getArgumentArray()[3], "the fourth argument must be the shipped default");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @DisplayName("resolve falls back to the shipped default for a non-numeric value")
    @Test
    void resolveCooldownMillisFallsBackForANonNumericValue() {
        long result = freshFallback().resolve(TickleSettings.COOLDOWN_KEY, "abc", TickleSettings::cooldownMillis, () -> 4000L);

        Assertions.assertEquals(4000L, result, "a non-numeric value must fall back to the shipped default too");
    }
}
