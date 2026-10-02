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
package net.onelitefeather.titan.feature.jumprun;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;

/**
 * Contributes the {@code titan:jumprun} item. The name is the same in every language and there is
 * no lore, because one stack is shared by all players.
 */
@Factory
final class JumprunItems {

    private static final Key ITEM_KEY = Key.key("titan:jumprun");
    private static final int HOTBAR_SLOT = 0;
    private static final Component NAME = MiniMessage.miniMessage().deserialize("<!i><gradient:#7CFC00:#00C853><b>Jump & Run</b></gradient>");

    @Bean
    LobbyItem jumprun(JumprunModule jumprun) {
        ItemStack slimeBlock = ItemStack.builder(Material.SLIME_BLOCK).customName(NAME).build();
        return new LobbyItem(JumprunModule.ID, ITEM_KEY, slimeBlock, ItemSlot.hotbar(HOTBAR_SLOT), (player, event) -> jumprun.toggle(player));
    }
}
