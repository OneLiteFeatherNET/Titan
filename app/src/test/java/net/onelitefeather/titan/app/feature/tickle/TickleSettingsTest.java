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

import java.time.Duration;
import net.onelitefeather.titan.common.config.ConfigException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TickleSettings#cooldown(long)}: the {@code cooldownMillis} validation
 * described in the {@code lobby-module-config} spec ("Negative Dauer" scenario). No {@code Config}
 * and no server involved.
 */
class TickleSettingsTest {

    @DisplayName("A zero cooldown is valid")
    @Test
    void zeroCooldownIsValid() {
        Assertions.assertEquals(Duration.ZERO, TickleSettings.cooldown(0));
    }

    @DisplayName("A positive cooldown is valid")
    @Test
    void positiveCooldownIsValid() {
        Assertions.assertEquals(Duration.ofMillis(1500), TickleSettings.cooldown(1500));
    }

    @DisplayName("A negative cooldown is rejected, naming the full key and reason")
    @Test
    void negativeCooldownIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> TickleSettings.cooldown(-5));

        Assertions.assertEquals("tickle.cooldownMillis", thrown.field());
        Assertions.assertEquals("must not be negative", thrown.reason());
    }
}
