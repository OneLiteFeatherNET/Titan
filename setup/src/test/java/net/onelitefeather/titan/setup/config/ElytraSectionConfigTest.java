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

import net.onelitefeather.titan.common.config.ConfigException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link ElytraSectionConfig}'s compact constructor validation - the same rule as
 * {@code net.onelitefeather.titan.app.feature.elytra.ElytraConfig}'s own compact constructor,
 * mirrored here so the setup server (which does not depend on {@code app}) can reject a pair the
 * lobby would refuse to load before it is ever written to {@code app.json}. See {@link
 * SetupConfigEditorTest} for that write-time rejection.
 */
class ElytraSectionConfigTest {

    @DisplayName("DEFAULTS matches the lobby's own ElytraConfig.DEFAULTS (30 / 40 ticks)")
    @Test
    void defaultsMatchesTheLobbysOwnTuning() {
        Assertions.assertEquals(30, ElytraSectionConfig.DEFAULTS.burnDurationTicks());
        Assertions.assertEquals(40, ElytraSectionConfig.DEFAULTS.cooldownTicks());
    }

    @DisplayName("A cooldown strictly longer than the burn is accepted unchanged")
    @Test
    void aCooldownStrictlyLongerThanTheBurnIsAccepted() {
        ElytraSectionConfig config = new ElytraSectionConfig(10, 15);

        Assertions.assertEquals(10, config.burnDurationTicks());
        Assertions.assertEquals(15, config.cooldownTicks());
    }

    @DisplayName("A zero burn duration is rejected")
    @Test
    void aZeroBurnDurationIsRejected() {
        ConfigException exception = Assertions.assertThrows(ConfigException.class, () -> new ElytraSectionConfig(0, 10));

        Assertions.assertEquals("burnDurationTicks", exception.field());
        Assertions.assertEquals("must be positive", exception.reason());
    }

    @DisplayName("A negative burn duration is rejected")
    @Test
    void aNegativeBurnDurationIsRejected() {
        ConfigException exception = Assertions.assertThrows(ConfigException.class, () -> new ElytraSectionConfig(-1, 10));

        Assertions.assertEquals("burnDurationTicks", exception.field());
    }

    @DisplayName("A cooldown equal to the burn duration is rejected")
    @Test
    void aCooldownEqualToTheBurnDurationIsRejected() {
        ConfigException exception = Assertions.assertThrows(ConfigException.class, () -> new ElytraSectionConfig(10, 10));

        Assertions.assertEquals("cooldownTicks", exception.field());
        Assertions.assertEquals("must be longer than burnDurationTicks", exception.reason());
    }

    @DisplayName("A cooldown shorter than the burn duration is rejected")
    @Test
    void aCooldownShorterThanTheBurnDurationIsRejected() {
        ConfigException exception = Assertions.assertThrows(ConfigException.class, () -> new ElytraSectionConfig(10, 5));

        Assertions.assertEquals("cooldownTicks", exception.field());
    }
}
