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
package net.onelitefeather.titan.app.feature.elytra;

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
 * Unit coverage for {@link ElytraSettings}'s validation of {@code burnDurationTicks} and
 * {@code cooldownTicks} - no {@code Config} and no server involved.
 */
class ElytraSettingsTest {

    @DisplayName("A cooldown strictly longer than the burn is accepted unchanged")
    @Test
    void aCooldownStrictlyLongerThanTheBurnIsAccepted() {
        int burnDurationTicks = ElytraSettings.burnDurationTicks("10");
        int cooldownTicks = ElytraSettings.cooldownTicks(15, burnDurationTicks);

        Assertions.assertEquals(10, burnDurationTicks);
        Assertions.assertEquals(15, cooldownTicks);
    }

    @DisplayName("A zero burn duration is rejected")
    @Test
    void aZeroBurnDurationIsRejected() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> ElytraSettings.burnDurationTicks("0"));

        Assertions.assertTrue(exception.getMessage().contains("positive"), "the message must explain why, was: " + exception.getMessage());
    }

    @DisplayName("A negative burn duration is rejected")
    @Test
    void aNegativeBurnDurationIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ElytraSettings.burnDurationTicks("-1"));
    }

    @DisplayName("A non-numeric burn duration fails to parse")
    @Test
    void nonNumericBurnDurationFailsToParse() {
        Assertions.assertThrows(NumberFormatException.class, () -> ElytraSettings.burnDurationTicks("abc"));
    }

    @DisplayName("A cooldown equal to the burn duration is rejected, naming both full keys")
    @Test
    void aCooldownEqualToTheBurnDurationIsRejected() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> ElytraSettings.cooldownTicks(10, 10));

        Assertions.assertTrue(exception.getMessage().contains(ElytraSettings.COOLDOWN_TICKS_KEY), "the message must name " + ElytraSettings.COOLDOWN_TICKS_KEY);
        Assertions.assertTrue(exception.getMessage().contains(ElytraSettings.BURN_DURATION_TICKS_KEY), "the message must name " + ElytraSettings.BURN_DURATION_TICKS_KEY);
    }

    @DisplayName("A cooldown shorter than the burn duration is rejected, naming both full keys")
    @Test
    void aCooldownShorterThanTheBurnDurationIsRejected() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> ElytraSettings.cooldownTicks(5, 10));

        Assertions.assertTrue(exception.getMessage().contains(ElytraSettings.COOLDOWN_TICKS_KEY), "the message must name " + ElytraSettings.COOLDOWN_TICKS_KEY);
        Assertions.assertTrue(exception.getMessage().contains(ElytraSettings.BURN_DURATION_TICKS_KEY), "the message must name " + ElytraSettings.BURN_DURATION_TICKS_KEY);
    }

    private static RuntimeConfigFallback freshFallback() {
        return new RuntimeConfigFallback(Configuration.builder().build());
    }

    @DisplayName("parseBoostSettings passes a valid pair through unchanged")
    @Test
    void parseBoostSettingsPassesAValidPairThrough() {
        ElytraSettings.BoostSettings result = ElytraSettings.parseBoostSettings(new ElytraSettings.RawBoostSettings("30", "40"));

        Assertions.assertEquals(30, result.burnDurationTicks());
        Assertions.assertEquals(40, result.cooldownTicks());
    }

    @DisplayName("parseBoostSettings rejects a cooldown not longer than the burn, naming both full keys")
    @Test
    void parseBoostSettingsRejectsACooldownNotLongerThanTheBurn() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> ElytraSettings.parseBoostSettings(new ElytraSettings.RawBoostSettings("30", "10")));

        Assertions.assertTrue(thrown.getMessage().contains(ElytraSettings.COOLDOWN_TICKS_KEY), "the message must name " + ElytraSettings.COOLDOWN_TICKS_KEY);
        Assertions.assertTrue(thrown.getMessage().contains(ElytraSettings.BURN_DURATION_TICKS_KEY), "the message must name " + ElytraSettings.BURN_DURATION_TICKS_KEY);
    }

    @DisplayName("resolve passes a valid boost settings pair through unchanged, without warning")
    @Test
    void resolveBoostSettingsPassesAValidPairThrough() {
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeConfigFallback.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            ElytraSettings.BoostSettings result = freshFallback().resolve(ElytraSettings.BOOST_SETTINGS_KEY, new ElytraSettings.RawBoostSettings("30", "40"), ElytraSettings::parseBoostSettings, () -> new ElytraSettings.BoostSettings(1, 2));

            Assertions.assertEquals(30, result.burnDurationTicks());
            Assertions.assertEquals(40, result.cooldownTicks());
            Assertions.assertTrue(appender.list.isEmpty(), "a valid pair must never warn");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @DisplayName("resolve falls back to the shipped boost settings and warns once when the cooldown is not longer than the burn")
    @Test
    void resolveBoostSettingsFallsBackAndWarnsWhenNotLongerThanTheBurn() {
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeConfigFallback.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            ElytraSettings.BoostSettings shippedDefault = new ElytraSettings.BoostSettings(30, 40);
            ElytraSettings.BoostSettings result = freshFallback().resolve(ElytraSettings.BOOST_SETTINGS_KEY, new ElytraSettings.RawBoostSettings("30", "10"), ElytraSettings::parseBoostSettings, () -> shippedDefault);

            Assertions.assertEquals(shippedDefault, result, "an invalid pair must fall back to the shipped boost settings");
            Assertions.assertEquals(1, appender.list.size(), "exactly one WARN must be logged for a new invalid pair");
            Assertions.assertEquals(ElytraSettings.BOOST_SETTINGS_KEY, appender.list.get(0).getArgumentArray()[0]);
        } finally {
            logger.detachAppender(appender);
        }
    }

    @DisplayName("shippedBoostSettings reads burnDurationTicks and cooldownTicks from the given configuration")
    @Test
    void shippedBoostSettingsReadsFromTheGivenConfiguration() {
        Configuration shipped = Configuration.builder().put(ElytraSettings.BURN_DURATION_TICKS_KEY, "30").put(ElytraSettings.COOLDOWN_TICKS_KEY, "40").build();

        ElytraSettings.BoostSettings result = ElytraSettings.shippedBoostSettings(shipped);

        Assertions.assertEquals(30, result.burnDurationTicks());
        Assertions.assertEquals(40, result.cooldownTicks());
    }
}
