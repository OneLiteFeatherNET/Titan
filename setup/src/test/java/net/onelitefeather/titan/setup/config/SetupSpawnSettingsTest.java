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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link SetupSpawnSettings#simulationDistance(int)}, the pure validation behind {@link
 * SetupSpawnConfig}'s compact constructor (design.md, decisions 3 and 5).
 *
 * <p>None of these tests touch the {@code io.avaje.config.Config} facade - the validated value
 * comes in as a plain {@code int}, so this class is free of the facade's global state.
 */
class SetupSpawnSettingsTest {

    @Test
    @DisplayName("Accepts a positive simulation distance")
    void acceptsAPositiveSimulationDistance() {
        assertEquals(4, SetupSpawnSettings.simulationDistance(4));
    }

    @Test
    @DisplayName("Rejects a simulation distance of zero, naming the full key")
    void rejectsAZeroSimulationDistance() {
        ConfigException thrown = assertThrows(ConfigException.class, () -> SetupSpawnSettings.simulationDistance(0));

        assertEquals("spawn.simulationDistance", thrown.field());
        assertTrue(thrown.getMessage().contains("spawn.simulationDistance"), "message must name the full key: " + thrown.getMessage());
    }

    @Test
    @DisplayName("Rejects a negative simulation distance, naming the full key")
    void rejectsANegativeSimulationDistance() {
        ConfigException thrown = assertThrows(ConfigException.class, () -> SetupSpawnSettings.simulationDistance(-1));

        assertEquals("spawn.simulationDistance", thrown.field());
        assertTrue(thrown.getMessage().contains("spawn.simulationDistance"), "message must name the full key: " + thrown.getMessage());
    }
}
