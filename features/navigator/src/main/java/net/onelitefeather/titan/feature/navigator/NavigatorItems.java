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
package net.onelitefeather.titan.feature.navigator;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;

/**
 * Contributes the {@code titan:navigator} feather to the platform-wide {@link
 * net.onelitefeather.titan.core.module.item.LobbyItems} as a {@code @Bean}.
 *
 * <p>Package-private, like the rest of this feature's internals: Avaje Inject's generated wiring
 * lives in the same package as the class it annotates, so it reaches
 * {@link #navigatorFeather(NavigatorModule)} without either needing to be public.
 */
@Factory
final class NavigatorItems {

    private static final String FEATURE_ID = "navigator";
    private static final Key ITEM_KEY = Key.key("titan:navigator");
    private static final int HOTBAR_SLOT = 4;

    @Bean
    LobbyItem navigatorFeather(NavigatorModule navigator) {
        ItemStack feather = ItemStack.builder(Material.FEATHER).customName(MiniMessage.miniMessage().deserialize("<!i><aqua>Navigator")).build();
        return new LobbyItem(FEATURE_ID, ITEM_KEY, feather, ItemSlot.hotbar(HOTBAR_SLOT), (player, event) -> navigator.open(player));
    }
}
