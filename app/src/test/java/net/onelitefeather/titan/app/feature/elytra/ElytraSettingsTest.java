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

import net.onelitefeather.titan.common.config.ConfigException;
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
        int burnDurationTicks = ElytraSettings.burnDurationTicks(10);
        int cooldownTicks = ElytraSettings.cooldownTicks(15, burnDurationTicks);

        Assertions.assertEquals(10, burnDurationTicks);
        Assertions.assertEquals(15, cooldownTicks);
    }

    @DisplayName("A zero burn duration is rejected")
    @Test
    void aZeroBurnDurationIsRejected() {
        ConfigException exception = Assertions.assertThrows(ConfigException.class, () -> ElytraSettings.burnDurationTicks(0));

        Assertions.assertEquals("elytra.burnDurationTicks", exception.field());
        Assertions.assertEquals("must be positive", exception.reason());
    }

    @DisplayName("A negative burn duration is rejected")
    @Test
    void aNegativeBurnDurationIsRejected() {
        ConfigException exception = Assertions.assertThrows(ConfigException.class, () -> ElytraSettings.burnDurationTicks(-1));

        Assertions.assertEquals("elytra.burnDurationTicks", exception.field());
    }

    @DisplayName("A cooldown equal to the burn duration is rejected")
    @Test
    void aCooldownEqualToTheBurnDurationIsRejected() {
        ConfigException exception = Assertions.assertThrows(ConfigException.class, () -> ElytraSettings.cooldownTicks(10, 10));

        Assertions.assertEquals("elytra.cooldownTicks", exception.field());
        Assertions.assertEquals("must be longer than burnDurationTicks", exception.reason());
    }

    @DisplayName("A cooldown shorter than the burn duration is rejected")
    @Test
    void aCooldownShorterThanTheBurnDurationIsRejected() {
        ConfigException exception = Assertions.assertThrows(ConfigException.class, () -> ElytraSettings.cooldownTicks(5, 10));

        Assertions.assertEquals("elytra.cooldownTicks", exception.field());
    }
}
