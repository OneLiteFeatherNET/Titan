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

import java.util.List;
import java.util.Objects;

/**
 * A player option with a fixed list of whole-number levels, for example a volume. It is stored like
 * a switch, but is not shown in the settings menu; the feature that declares it offers the
 * controls.
 *
 * @param key          the stored key, in the same format as a switch
 * @param levels       the allowed levels in ascending order, without duplicates
 * @param defaultLevel the level a player has while nothing is stored; one of {@code levels}
 */
public record LevelSetting(SettingKey key, List<Integer> levels, int defaultLevel) {

    public LevelSetting {
        Objects.requireNonNull(key, "key must not be null");
        levels = List.copyOf(Objects.requireNonNull(levels, "levels must not be null")).stream().sorted().toList();
        if (levels.isEmpty()) {
            throw new IllegalArgumentException("a level setting needs at least one level");
        }
        if (levels.stream().distinct().count() != levels.size()) {
            throw new IllegalArgumentException("a level must not be listed twice: " + levels);
        }
        if (!levels.contains(defaultLevel)) {
            throw new IllegalArgumentException("the default " + defaultLevel + " is not one of the levels " + levels);
        }
    }

    /** Whether {@code level} is one of the allowed levels. */
    public boolean allows(int level) {
        return this.levels.contains(level);
    }
}
