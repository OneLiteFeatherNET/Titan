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
package net.onelitefeather.titan.feature.settings;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import io.avaje.inject.Secondary;
import jakarta.inject.Named;
import java.util.concurrent.Executor;
import net.onelitefeather.titan.core.settings.PlayerSettings;
import net.onelitefeather.titan.core.settings.SettingsRegistry;
import net.onelitefeather.titan.core.settings.SettingsStore;

/**
 * The settings column's own beans. The in-memory store is secondary: a platform module that
 * provides the Otis store replaces it, and a lobby without one keeps this.
 */
@Factory
final class SettingsBeans {

    /** Name of the executor loads and saves run on. */
    static final String IO = "settings.io";

    @Bean
    SettingsOptions settingsOptions() {
        return SettingsOptions.read();
    }

    @Bean
    SettingsRegistry settingsRegistry() {
        return new SettingsRegistry();
    }

    @Bean
    SettingsMessages settingsMessages() {
        return new SettingsMessages();
    }

    /**
     * A virtual thread per load or save: a store may block on the network, which a tick must not.
     */
    @Bean
    @Named(IO)
    Executor settingsIo() {
        return task -> Thread.ofVirtual().name("titan-settings-io").start(task);
    }

    @Bean
    @Secondary
    SettingsStore inMemorySettingsStore() {
        return new InMemorySettingsStore();
    }

    @Bean
    PlayerSettings playerSettings(SettingsModule module) {
        return module.playerSettings();
    }
}
