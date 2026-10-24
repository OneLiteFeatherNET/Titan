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
package net.onelitefeather.titan.core.settings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Every player option the lobby offers, in registration order. Columns register at startup; a
 * key taken twice is an error, because two features would read the same stored value.
 */
public final class SettingsRegistry {

    /** One menu row: the settings menu shows every option in a single row. */
    public static final int MAX_SETTINGS = 9;

    private final Map<SettingKey, Registration> registrations = new LinkedHashMap<>();

    /**
     * @param owner the feature registering the option; named in the error if the key is taken
     * @throws IllegalStateException if the key is already registered or the registry is full
     */
    public synchronized void register(String owner, BooleanSetting setting) {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(setting, "setting must not be null");
        Registration existing = this.registrations.get(setting.key());
        if (existing != null) {
            throw new IllegalStateException("Setting " + setting.key() + " is registered twice: by '" + existing.owner() + "' and by '" + owner + "'");
        }
        if (this.registrations.size() >= MAX_SETTINGS) {
            throw new IllegalStateException("At most " + MAX_SETTINGS + " settings fit the settings menu, cannot add " + setting.key());
        }
        this.registrations.put(setting.key(), new Registration(owner, setting));
    }

    public synchronized Optional<BooleanSetting> find(SettingKey key) {
        return Optional.ofNullable(this.registrations.get(key)).map(Registration::setting);
    }

    /** @return the registered options in registration order */
    public synchronized List<BooleanSetting> definitions() {
        return this.registrations.values().stream().map(Registration::setting).toList();
    }

    private record Registration(String owner, BooleanSetting setting) {
    }
}
