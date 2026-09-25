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
package net.onelitefeather.titan.app.feature.spawn;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain unit tests for what is still specific to the {@link SpawnConfig} record itself - its
 * documented {@link SpawnConfig#DEFAULTS}. The validation cases now live in
 * {@link SpawnSettingsTest}.
 */
class SpawnConfigTest {

    @DisplayName("DEFAULTS matches the documented spawn defaults (-64, 310, 2)")
    @Test
    void defaultsMatchTheDocumentedValues() {
        Assertions.assertEquals(-64, SpawnConfig.DEFAULTS.minHeight());
        Assertions.assertEquals(310, SpawnConfig.DEFAULTS.maxHeight());
        Assertions.assertEquals(2, SpawnConfig.DEFAULTS.simulationDistance());
    }

    @DisplayName("A valid config does not throw")
    @Test
    void aValidConfigDoesNotThrow() {
        Assertions.assertDoesNotThrow(() -> new SpawnConfig(-64, 310, 2));
    }
}
