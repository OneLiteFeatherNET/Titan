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
package net.onelitefeather.titan.feature.radio;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.onelitefeather.titan.core.settings.BooleanSetting;
import net.onelitefeather.titan.core.settings.LevelSetting;
import net.onelitefeather.titan.core.settings.PlayerSettings;
import net.onelitefeather.titan.core.settings.SettingChangeListener;

/** The player settings of a test: values live in memory for the test's own module only. */
final class InMemoryRadioSettings implements PlayerSettings {

    private final Map<UUID, Integer> levels = new HashMap<>();

    @Override
    public boolean isEnabled(UUID player, BooleanSetting setting) {
        return setting.defaultValue();
    }

    @Override
    public void set(UUID player, BooleanSetting setting, boolean enabled) {
        // The radio test does not switch booleans.
    }

    @Override
    public int level(UUID player, LevelSetting setting) {
        return this.levels.getOrDefault(player, setting.defaultLevel());
    }

    @Override
    public void setLevel(UUID player, LevelSetting setting, int level) {
        if (!setting.allows(level)) {
            throw new IllegalArgumentException(level + " is not a level of " + setting.key());
        }
        this.levels.put(player, level);
    }

    @Override
    public void addChangeListener(SettingChangeListener listener) {
        // The radio test does not listen to switches.
    }
}
