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

    /** One menu row: the settings menu shows every switch in a single row. */
    public static final int MAX_SETTINGS = 9;

    private final Map<SettingKey, Registration> registrations = new LinkedHashMap<>();
    private final Map<SettingKey, LevelRegistration> levels = new LinkedHashMap<>();

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
        checkFreeOfLevel(setting.key());
        if (this.registrations.size() >= MAX_SETTINGS) {
            throw new IllegalStateException("At most " + MAX_SETTINGS + " settings fit the settings menu, cannot add " + setting.key());
        }
        this.registrations.put(setting.key(), new Registration(owner, setting));
    }

    /**
     * Registers a level option. It is stored and loaded like a switch, but it is not a menu row, so
     * it does not count against {@link #MAX_SETTINGS}.
     *
     * @throws IllegalStateException if the key is already registered, as a switch or a level
     */
    public synchronized void registerLevel(String owner, LevelSetting setting) {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(setting, "setting must not be null");
        LevelRegistration existing = this.levels.get(setting.key());
        if (existing != null) {
            throw new IllegalStateException("Setting " + setting.key() + " is registered twice: by '" + existing.owner() + "' and by '" + owner + "'");
        }
        Registration asSwitch = this.registrations.get(setting.key());
        if (asSwitch != null) {
            throw new IllegalStateException("Setting " + setting.key() + " is a switch of '" + asSwitch.owner() + "', cannot be a level for '" + owner + "'");
        }
        this.levels.put(setting.key(), new LevelRegistration(owner, setting));
    }

    /** @return the setting registered under the key, if any */
    public synchronized Optional<BooleanSetting> find(SettingKey key) {
        return Optional.ofNullable(this.registrations.get(key)).map(Registration::setting);
    }

    /** @return the level registered under the key, if any */
    public synchronized Optional<LevelSetting> findLevel(SettingKey key) {
        return Optional.ofNullable(this.levels.get(key)).map(LevelRegistration::setting);
    }

    /** @return the registered switches in registration order; levels are not shown in the menu */
    public synchronized List<BooleanSetting> definitions() {
        return this.registrations.values().stream().map(Registration::setting).toList();
    }

    /** @return the registered levels in registration order, as the store loads them */
    public synchronized List<LevelSetting> levelDefinitions() {
        return this.levels.values().stream().map(LevelRegistration::setting).toList();
    }

    private void checkFreeOfLevel(SettingKey key) {
        LevelRegistration asLevel = this.levels.get(key);
        if (asLevel != null) {
            throw new IllegalStateException("Setting " + key + " is a level of '" + asLevel.owner() + "', cannot be a switch");
        }
    }

    private record Registration(String owner, BooleanSetting setting) {
    }

    private record LevelRegistration(String owner, LevelSetting setting) {
    }
}
