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

import io.avaje.config.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
