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
 * Plain unit tests for {@link SpawnSettings}: no {@code Config}, no server needed - just the pure
 * parsing and validation functions.
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
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> SpawnSettings.minHeight(100, 100));

        Assertions.assertTrue(thrown.getMessage().contains(SpawnSettings.MIN_HEIGHT_KEY), "the message must name " + SpawnSettings.MIN_HEIGHT_KEY);
        Assertions.assertTrue(thrown.getMessage().contains(SpawnSettings.MAX_HEIGHT_KEY), "the message must name " + SpawnSettings.MAX_HEIGHT_KEY);
    }

    @DisplayName("minHeight above maxHeight is rejected, naming both full keys")
    @Test
    void minHeightAboveMaxHeightIsRejected() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> SpawnSettings.minHeight(400, 300));

        Assertions.assertTrue(thrown.getMessage().contains(SpawnSettings.MIN_HEIGHT_KEY), "the message must name " + SpawnSettings.MIN_HEIGHT_KEY);
        Assertions.assertTrue(thrown.getMessage().contains(SpawnSettings.MAX_HEIGHT_KEY), "the message must name " + SpawnSettings.MAX_HEIGHT_KEY);
    }

    @DisplayName("A valid simulationDistance is returned unchanged")
    @Test
    void aValidSimulationDistanceIsReturnedUnchanged() {
        Assertions.assertEquals(2, SpawnSettings.simulationDistance("2"));
    }

    @DisplayName("A zero simulationDistance is rejected")
    @Test
    void zeroSimulationDistanceIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SpawnSettings.simulationDistance("0"));
    }

    @DisplayName("A negative simulationDistance is rejected")
    @Test
    void negativeSimulationDistanceIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SpawnSettings.simulationDistance("-1"));
    }

    @DisplayName("A non-numeric simulationDistance fails to parse")
    @Test
    void nonNumericSimulationDistanceFailsToParse() {
        Assertions.assertThrows(NumberFormatException.class, () -> SpawnSettings.simulationDistance("abc"));
    }
}
