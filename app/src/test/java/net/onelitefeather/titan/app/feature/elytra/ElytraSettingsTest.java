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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
