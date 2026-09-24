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
 * Unit coverage for {@link ElytraConfig}'s compact constructor validation and its defaults - no
 * {@code Env} or server required.
 */
class ElytraConfigTest {

    @DisplayName("DEFAULTS matches today's shipped boost multiplier (35.0, not vanilla's 1.0)")
    @Test
    void defaultsMatchesTodaysShippedBoostMultiplier() {
        Assertions.assertEquals(35.0, ElytraConfig.DEFAULTS.boostMultiplier());
    }

    @DisplayName("A positive boost multiplier is accepted unchanged")
    @Test
    void aPositiveBoostMultiplierIsAccepted() {
        ElytraConfig config = new ElytraConfig(2.5);

        Assertions.assertEquals(2.5, config.boostMultiplier());
    }

    @DisplayName("A zero boost multiplier is rejected")
    @Test
    void aZeroBoostMultiplierIsRejected() {
        ConfigException exception = Assertions.assertThrows(ConfigException.class, () -> new ElytraConfig(0.0));

        Assertions.assertEquals("boostMultiplier", exception.field());
    }

    @DisplayName("A negative boost multiplier is rejected")
    @Test
    void aNegativeBoostMultiplierIsRejected() {
        ConfigException exception = Assertions.assertThrows(ConfigException.class, () -> new ElytraConfig(-1.0));

        Assertions.assertEquals("boostMultiplier", exception.field());
        Assertions.assertEquals("must be positive", exception.reason());
    }
}
