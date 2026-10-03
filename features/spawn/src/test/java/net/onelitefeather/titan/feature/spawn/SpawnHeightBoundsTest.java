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
package net.onelitefeather.titan.feature.spawn;

import io.avaje.config.Config;
import net.onelitefeather.titan.core.module.LobbyHeightBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the {@link LobbyHeightBounds} bean the spawn column provides: it reflects the {@code
 * spawn} section's values and follows later changes to them.
 */
class SpawnHeightBoundsTest {

    private String originalMin;
    private String originalMax;

    @BeforeEach
    void rememberOriginals() {
        this.originalMin = Config.get(SpawnSettings.MIN_HEIGHT_KEY);
        this.originalMax = Config.get(SpawnSettings.MAX_HEIGHT_KEY);
    }

    @AfterEach
    void restoreOriginals() {
        Config.setProperty(SpawnSettings.MIN_HEIGHT_KEY, this.originalMin);
        Config.setProperty(SpawnSettings.MAX_HEIGHT_KEY, this.originalMax);
    }

    @DisplayName("The bean returns the configured minimum and maximum height")
    @Test
    void returnsConfiguredBounds() {
        Config.setProperty(SpawnSettings.MIN_HEIGHT_KEY, "-10");
        Config.setProperty(SpawnSettings.MAX_HEIGHT_KEY, "200");

        LobbyHeightBounds bounds = new SpawnHeightBoundsFactory().lobbyHeightBounds();

        Assertions.assertEquals(-10, bounds.minHeight(), "minHeight must come from " + SpawnSettings.MIN_HEIGHT_KEY);
        Assertions.assertEquals(200, bounds.maxHeight(), "maxHeight must come from " + SpawnSettings.MAX_HEIGHT_KEY);
    }

    @DisplayName("The bean returns the new values after the configuration changed")
    @Test
    void followsConfigurationChanges() {
        LobbyHeightBounds bounds = new SpawnHeightBoundsFactory().lobbyHeightBounds();
        Config.setProperty(SpawnSettings.MIN_HEIGHT_KEY, "5");
        Config.setProperty(SpawnSettings.MAX_HEIGHT_KEY, "99");

        Assertions.assertEquals(5, bounds.minHeight(), "minHeight must be read live");
        Assertions.assertEquals(99, bounds.maxHeight(), "maxHeight must be read live");
    }
}
