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
 * Unit test for {@link ElytraConfig}'s own documented default. The {@code burnDurationTicks} and
 * {@code cooldownTicks} validation itself is {@link ElytraSettings}'s responsibility and is
 * covered by {@link ElytraSettingsTest}; {@link ElytraConfig}'s compact constructor only delegates
 * to it.
 */
class ElytraConfigTest {

    @DisplayName("DEFAULTS matches Voyager's own deterministic burn and reference-map cooldown (30 / 40 ticks)")
    @Test
    void defaultsMatchesVoyagersOwnTuning() {
        Assertions.assertEquals(30, ElytraConfig.DEFAULTS.burnDurationTicks());
        Assertions.assertEquals(40, ElytraConfig.DEFAULTS.cooldownTicks());
    }
}
