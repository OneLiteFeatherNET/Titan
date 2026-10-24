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
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;

/**
 * Contributes the {@code titan:settings} item to the platform-wide lobby items, only while the
 * feature is on. An empty Optional is no bean at all, so the hotbar never shows the item.
 */
@Factory
final class SettingsItems {

    private static final String FEATURE_ID = "settings";
    private static final Key ITEM_KEY = Key.key("titan:settings");
    private static final int HOTBAR_SLOT = 7;

    @Bean
    Optional<LobbyItem> settingsItem(SettingsModule module, SettingsOptions options, SettingsMessages messages) {
        if (!options.enabled()) {
            return Optional.empty();
        }
        // The name is rendered per locale when the hotbar equips the player, not here: the translations
        // are registered when the module starts, which may be after this bean is built.
        ItemStack comparator = ItemStack.builder(Material.COMPARATOR).build();
        return Optional.of(new LobbyItem(FEATURE_ID, ITEM_KEY, comparator, ItemSlot.hotbar(HOTBAR_SLOT), (player, event) -> module.open(player), locale -> ItemStack.builder(Material.COMPARATOR).customName(messages.itemName(locale)).build()));
    }
}
