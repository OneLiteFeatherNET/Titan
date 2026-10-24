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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.onelitefeather.titan.core.settings.SettingKey;
import net.onelitefeather.titan.core.settings.SettingsStore;

/**
 * The store of a lobby without an Otis url: values survive a visit to the lobby, not a restart of
 * the process.
 */
final class InMemorySettingsStore implements SettingsStore {

    private final Map<UUID, Map<SettingKey, Boolean>> values = new ConcurrentHashMap<>();

    @Override
    public Map<SettingKey, Boolean> load(UUID player) {
        return Map.copyOf(this.values.getOrDefault(player, Map.of()));
    }

    @Override
    public void save(UUID player, SettingKey key, boolean enabled) {
        this.values.computeIfAbsent(player, ignored -> new ConcurrentHashMap<>()).put(key, enabled);
    }
}
