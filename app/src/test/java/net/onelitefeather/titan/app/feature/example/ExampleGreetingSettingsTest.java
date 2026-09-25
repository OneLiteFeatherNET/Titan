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
package net.onelitefeather.titan.app.feature.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain unit coverage for {@link ExampleGreetingSettings}'s pure validation - no {@code Config}, no
 * server, see {@code docs/lobby-modules.md}, "Unten: reine Unit-Tests".
 */
class ExampleGreetingSettingsTest {

    @DisplayName("ExampleModule's own defaults are themselves valid")
    @Test
    void moduleDefaultsAreValid() {
        Assertions.assertDoesNotThrow(() -> ExampleGreetingSettings.greeting(ExampleModule.DEFAULT_GREETING));
        Assertions.assertDoesNotThrow(() -> ExampleGreetingSettings.cooldownMillis(String.valueOf(ExampleModule.DEFAULT_COOLDOWN_MILLIS)));
    }

    @DisplayName("A valid greeting passes through unchanged")
    @Test
    void aValidGreetingPassesThroughUnchanged() {
        Assertions.assertEquals("Hi %s!", ExampleGreetingSettings.greeting("Hi %s!"));
    }

    @DisplayName("A blank greeting is rejected")
    @Test
    void blankGreetingIsRejected() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> ExampleGreetingSettings.greeting("   "));

        Assertions.assertEquals("must not be blank", thrown.getMessage());
    }

    @DisplayName("A greeting without a %s placeholder is rejected")
    @Test
    void greetingWithoutPlaceholderIsRejected() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> ExampleGreetingSettings.greeting("Welcome to the lobby!"));

        Assertions.assertEquals("must contain a '%s' placeholder for the player's name", thrown.getMessage());
    }

    @DisplayName("A non-negative cooldown passes through unchanged")
    @Test
    void aNonNegativeCooldownPassesThroughUnchanged() {
        Assertions.assertEquals(0L, ExampleGreetingSettings.cooldownMillis("0"));
    }

    @DisplayName("A negative cooldown is rejected")
    @Test
    void negativeCooldownIsRejected() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> ExampleGreetingSettings.cooldownMillis("-1"));

        Assertions.assertTrue(thrown.getMessage().contains("-1"), "the message must keep the offending value, was: " + thrown.getMessage());
    }

    @DisplayName("A non-numeric cooldown fails to parse")
    @Test
    void nonNumericCooldownFailsToParse() {
        Assertions.assertThrows(NumberFormatException.class, () -> ExampleGreetingSettings.cooldownMillis("abc"));
    }
}
