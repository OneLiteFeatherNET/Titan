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
package net.onelitefeather.titan.platform.otis;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import java.time.Clock;
import java.util.Optional;
import net.onelitefeather.titan.core.settings.SettingsStore;
import net.onelitefeather.titan.core.settings.SettingsTelemetry;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Provides the Otis store as a bean. An empty Optional when {@code otis.url} is blank, so the
 * settings column keeps its in-memory store.
 */
@Factory
final class OtisBeans {

    @Bean
    Optional<SettingsStore> otisSettingsStore(Clock clock, Telemetry telemetry) {
        OtisSettings settings = OtisSettings.read();
        if (settings.url().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(OtisSettingsStore.create(settings, clock, new SettingsTelemetry(telemetry)));
    }
}
