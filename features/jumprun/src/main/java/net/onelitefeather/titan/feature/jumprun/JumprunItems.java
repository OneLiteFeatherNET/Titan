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
import java.util.List;
import java.util.stream.Stream;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;

/**
 * Contributes the {@code titan:jumprun} item. One stack is shared by all players, so name and lore
 * are the same in every language (lore is English on purpose).
 */
@Factory
final class JumprunItems {

    private static final Key ITEM_KEY = Key.key("titan:jumprun");
    private static final int HOTBAR_SLOT = 0;
    private static final Component NAME = MiniMessage.miniMessage().deserialize("<!i>" + RunTitle.MARKUP);
    private static final List<Component> LORE = Stream.of(
            "<!i><gray>Right-click: start / stop a run", "<!i><gray>Sneak + right-click: switch mode", "<!i>", "<!i><green>Easy:</green> <gray>full blocks and slabs, short gaps", "<!i><yellow>Medium:</yellow> <gray>all shapes, steady climb", "<!i><red>Hard:</red> <gray>all shapes early, fast climb", "<!i><rainbow>Rainbow:</rainbow> <gray>blocks change colour while you stand", "<!i><dark_red><b>Ultra:</b></dark_red> <gray>blocks reshuffle while you stand").map(MiniMessage.miniMessage()::deserialize).toList();

    @Bean
    LobbyItem jumprun(JumprunModule jumprun) {
        ItemStack slimeBlock = ItemStack.builder(Material.SLIME_BLOCK).customName(NAME).lore(LORE).build();
        return new LobbyItem(JumprunModule.ID, ITEM_KEY, slimeBlock, ItemSlot.hotbar(HOTBAR_SLOT), (player, event) -> jumprun.use(player));
    }
}
