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

import java.util.Objects;

/**
 * A player option that is either on or off. A column declares one, registers it in the
 * {@link SettingsRegistry} at startup, and reads the player's value through {@link PlayerSettings}.
 */
public record BooleanSetting(SettingKey key, SettingDefault defaultMode) {

    public BooleanSetting {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(defaultMode, "defaultMode must not be null");
    }

    /** The value a player has while no value is stored for them. */
    public boolean defaultValue() {
        return this.defaultMode.initialValue();
    }
}
