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
package net.onelitefeather.titan.feature.throwing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.onelitefeather.titan.core.settings.BooleanSetting;
import net.onelitefeather.titan.core.settings.LevelSetting;
import net.onelitefeather.titan.core.settings.PlayerSettings;
import net.onelitefeather.titan.core.settings.SettingChangeListener;

/** An in-memory settings port that reports changes like the real one does. */
final class FakePlayerSettings implements PlayerSettings {

    private final Map<UUID, Map<BooleanSetting, Boolean>> values = new HashMap<>();
    private final List<SettingChangeListener> listeners = new ArrayList<>();

    @Override
    public boolean isEnabled(UUID player, BooleanSetting setting) {
        return this.values.getOrDefault(player, Map.of()).getOrDefault(setting, setting.defaultValue());
    }

    @Override
    public void set(UUID player, BooleanSetting setting, boolean enabled) {
        boolean previous = isEnabled(player, setting);
        this.values.computeIfAbsent(player, ignored -> new HashMap<>()).put(setting, enabled);
        if (previous != enabled) {
            this.listeners.forEach(listener -> listener.changed(player, setting, previous, enabled));
        }
    }

    @Override
    public int level(UUID player, LevelSetting setting) {
        return setting.defaultLevel();
    }

    @Override
    public void setLevel(UUID player, LevelSetting setting, int level) {
        throw new UnsupportedOperationException("the fake holds switches only");
    }

    @Override
    public void addChangeListener(SettingChangeListener listener) {
        this.listeners.add(listener);
    }
}
