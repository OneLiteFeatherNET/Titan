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
import io.avaje.config.Configuration;
import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import net.onelitefeather.titan.core.module.LobbyHeightBounds;

/**
 * Provides the lobby's height bounds to other columns as a {@link LobbyHeightBounds} bean, so
 * they never read the {@code spawn} section themselves. The spawn column's own bounds check uses
 * the same bean, which keeps a single source for both.
 *
 * <p>Package-private: Avaje Inject's generated wiring lives in this package.
 */
@Factory
final class SpawnHeightBoundsFactory {

    /** Reads the keys on every call, so a changed bound applies without a restart. */
    @Bean
    LobbyHeightBounds lobbyHeightBounds() {
        return boundsOf(Config.asConfiguration());
    }

    /** Takes the configuration as a parameter so a test can bring its own. */
    static LobbyHeightBounds boundsOf(Configuration config) {
        return new ConfiguredHeightBounds(config);
    }

    private record ConfiguredHeightBounds(Configuration config) implements LobbyHeightBounds {

        @Override
        public int minHeight() {
            return config.getInt(SpawnSettings.MIN_HEIGHT_KEY);
        }

        @Override
        public int maxHeight() {
            return config.getInt(SpawnSettings.MAX_HEIGHT_KEY);
        }
    }
}
