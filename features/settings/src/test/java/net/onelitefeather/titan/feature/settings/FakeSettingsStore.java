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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.onelitefeather.titan.core.settings.SettingKey;
import net.onelitefeather.titan.core.settings.SettingValue;
import net.onelitefeather.titan.core.settings.SettingsStore;

/**
 * An in-process store that counts its calls, so a test can see whether the module asked it at all.
 */
final class FakeSettingsStore implements SettingsStore {

    private final Map<UUID, Map<SettingKey, SettingValue>> stored = new ConcurrentHashMap<>();
    private final List<Save> saves = new CopyOnWriteArrayList<>();
    private final AtomicInteger loads = new AtomicInteger();

    FakeSettingsStore with(UUID player, SettingKey key, boolean value) {
        return with(player, key, new SettingValue.Flag(value));
    }

    FakeSettingsStore with(UUID player, SettingKey key, SettingValue value) {
        this.stored.computeIfAbsent(player, ignored -> new ConcurrentHashMap<>()).put(key, value);
        return this;
    }

    @Override
    public Map<SettingKey, SettingValue> load(UUID player) {
        this.loads.incrementAndGet();
        return Map.copyOf(this.stored.getOrDefault(player, Map.of()));
    }

    @Override
    public void save(UUID player, SettingKey key, SettingValue value) {
        this.saves.add(new Save(player, key, value));
        this.stored.computeIfAbsent(player, ignored -> new ConcurrentHashMap<>()).put(key, value);
    }

    int loads() {
        return this.loads.get();
    }

    List<Save> saves() {
        return new ArrayList<>(this.saves);
    }

    record Save(UUID player, SettingKey key, SettingValue value) {
    }
}
