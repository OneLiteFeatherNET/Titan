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
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.settings.SettingKey;

/**
 * The vanilla item that stands for an option's feature in the menu. A name tag stands for the tag
 * game; the ender pearl is the vanilla throwable, and it avoids the feather and slime ball the
 * navigator already uses. An option without an entry shows paper.
 */
final class SettingsIcons {

    static final Material FALLBACK = Material.PAPER;

    private static final Map<SettingKey, Material> ICONS = Map.of(SettingKey.parse("titan:tag.enabled"), Material.NAME_TAG, SettingKey.parse("titan:throw.target"), Material.ENDER_PEARL);

    private SettingsIcons() {
    }

    static Material of(SettingKey key) {
        return ICONS.getOrDefault(key, FALLBACK);
    }
}
