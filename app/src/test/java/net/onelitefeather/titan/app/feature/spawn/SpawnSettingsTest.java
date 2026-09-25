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

import net.onelitefeather.titan.common.config.ConfigException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain unit tests for {@link SpawnSettings}: no {@code Config}, no {@code ConfigSections}, no
 * server needed - just the pure validation functions.
 */
class SpawnSettingsTest {

    @DisplayName("A valid minHeight is returned unchanged")
    @Test
    void aValidMinHeightIsReturnedUnchanged() {
        Assertions.assertEquals(-64, SpawnSettings.minHeight(-64, 310));
    }

    @DisplayName("minHeight equal to maxHeight is rejected, naming both full keys")
    @Test
    void minHeightEqualToMaxHeightIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> SpawnSettings.minHeight(100, 100));

        Assertions.assertEquals("spawn.minHeight", thrown.field());
        Assertions.assertTrue(thrown.reason().contains("spawn.maxHeight"), "the reason must name spawn.maxHeight too");
    }

    @DisplayName("minHeight above maxHeight is rejected, naming both full keys")
    @Test
    void minHeightAboveMaxHeightIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> SpawnSettings.minHeight(400, 300));

        Assertions.assertEquals("spawn.minHeight", thrown.field());
        Assertions.assertTrue(thrown.reason().contains("spawn.maxHeight"), "the reason must name spawn.maxHeight too");
    }

    @DisplayName("A valid simulationDistance is returned unchanged")
    @Test
    void aValidSimulationDistanceIsReturnedUnchanged() {
        Assertions.assertEquals(2, SpawnSettings.simulationDistance(2));
    }

    @DisplayName("A zero simulationDistance is rejected, naming the full key")
    @Test
    void zeroSimulationDistanceIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> SpawnSettings.simulationDistance(0));

        Assertions.assertEquals("spawn.simulationDistance", thrown.field());
    }

    @DisplayName("A negative simulationDistance is rejected, naming the full key")
    @Test
    void negativeSimulationDistanceIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> SpawnSettings.simulationDistance(-1));

        Assertions.assertEquals("spawn.simulationDistance", thrown.field());
    }

    @DisplayName("A minHeight rejection message names spawn.minHeight exactly once")
    @Test
    void minHeightRejectionMessageNamesTheFullKeyExactlyOnce() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> SpawnSettings.minHeight(400, 300));
        String message = thrown.getMessage();
        int firstIndex = message.indexOf("spawn.minHeight");

        Assertions.assertTrue(firstIndex >= 0, "the message must name spawn.minHeight");
        Assertions.assertEquals(-1, message.indexOf("spawn.minHeight", firstIndex + 1), "spawn.minHeight must appear exactly once, was: " + message);
    }

    @DisplayName("A simulationDistance rejection message names spawn.simulationDistance exactly once")
    @Test
    void simulationDistanceRejectionMessageNamesTheFullKeyExactlyOnce() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> SpawnSettings.simulationDistance(0));
        String message = thrown.getMessage();
        int firstIndex = message.indexOf("spawn.simulationDistance");

        Assertions.assertTrue(firstIndex >= 0, "the message must name spawn.simulationDistance");
        Assertions.assertEquals(-1, message.indexOf("spawn.simulationDistance", firstIndex + 1), "spawn.simulationDistance must appear exactly once, was: " + message);
    }
}
