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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link TickleConfig}'s own documented default. The {@code cooldownMillis}
 * validation itself is {@link TickleSettings}'s responsibility and is covered by
 * {@link TickleSettingsTest}; {@link TickleConfig}'s compact constructor only delegates to it.
 */
class TickleConfigTest {

    @DisplayName("DEFAULTS has a 4000ms cooldown")
    @Test
    void defaultsHaveA4000MillisecondCooldown() {
        Assertions.assertEquals(4000L, TickleConfig.DEFAULTS.cooldownMillis());
    }
}
