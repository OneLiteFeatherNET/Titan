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

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import net.kyori.adventure.key.Key;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;

/**
 * Contributes the {@code titan:radio} jukebox to the platform-wide
 * {@link net.onelitefeather.titan.core.module.item.LobbyItems}. It is always there, since the
 * vanilla
 * discs need no files.
 */
@Factory
final class RadioItems {

    static final Key ITEM_KEY = Key.key("titan:radio");
    static final int HOTBAR_SLOT = 5;

    @Bean
    LobbyItem radioItem(RadioModule radio, RadioMessages messages) {
        ItemStack jukebox = ItemStack.builder(Material.JUKEBOX).build();
        return new LobbyItem(RadioModule.ID, ITEM_KEY, jukebox, ItemSlot.hotbar(HOTBAR_SLOT), (player, event) -> radio.open(player), locale -> ItemStack.builder(Material.JUKEBOX).customName(messages.itemName(locale)).lore(messages.itemHint(locale)).build());
    }
}
