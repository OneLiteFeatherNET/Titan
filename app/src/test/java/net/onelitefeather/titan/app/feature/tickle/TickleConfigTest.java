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

import net.onelitefeather.titan.common.config.ConfigException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TickleConfig}: the documented default and the {@code cooldownMillis}
 * validation described in the {@code lobby-module-config} spec ("Negative Dauer" scenario).
 */
class TickleConfigTest {

    @DisplayName("DEFAULTS has a 4000ms cooldown")
    @Test
    void defaultsHaveA4000MillisecondCooldown() {
        Assertions.assertEquals(4000L, TickleConfig.DEFAULTS.cooldownMillis());
    }

    @DisplayName("A zero cooldown is valid")
    @Test
    void zeroCooldownIsValid() {
        Assertions.assertDoesNotThrow(() -> new TickleConfig(0));
    }

    @DisplayName("A positive cooldown is valid")
    @Test
    void positiveCooldownIsValid() {
        Assertions.assertEquals(1500L, new TickleConfig(1500).cooldownMillis());
    }

    @DisplayName("A negative cooldown is rejected, naming the field and reason")
    @Test
    void negativeCooldownIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> new TickleConfig(-5));

        Assertions.assertEquals("cooldownMillis", thrown.field());
        Assertions.assertEquals("must not be negative", thrown.reason());
    }
}
