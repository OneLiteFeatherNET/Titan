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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers {@link SetupSpawnSettings#simulationDistance(String)}, the pure parsing and validation
 * behind {@link SetupSpawnConfig#read()} (design.md, decisions 3, 4 and 5).
 *
 * <p>None of these tests touch the {@code io.avaje.config.Config} facade - the raw value comes in
 * as a plain {@code String}, so this class is free of the facade's global state.
 */
class SetupSpawnSettingsTest {

    @Test
    @DisplayName("Accepts a positive simulation distance")
    void acceptsAPositiveSimulationDistance() {
        assertEquals(4, SetupSpawnSettings.simulationDistance("4"));
    }

    @Test
    @DisplayName("Rejects a simulation distance of zero")
    void rejectsAZeroSimulationDistance() {
        assertThrows(IllegalArgumentException.class, () -> SetupSpawnSettings.simulationDistance("0"));
    }

    @Test
    @DisplayName("Rejects a negative simulation distance")
    void rejectsANegativeSimulationDistance() {
        assertThrows(IllegalArgumentException.class, () -> SetupSpawnSettings.simulationDistance("-1"));
    }

    @Test
    @DisplayName("Rejects a non-numeric simulation distance")
    void rejectsANonNumericSimulationDistance() {
        assertThrows(NumberFormatException.class, () -> SetupSpawnSettings.simulationDistance("abc"));
    }
}
