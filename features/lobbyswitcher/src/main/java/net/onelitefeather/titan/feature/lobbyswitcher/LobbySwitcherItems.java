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
package net.onelitefeather.titan.feature.lobbyswitcher;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import io.avaje.inject.Profile;
import java.util.Locale;
import net.kyori.adventure.key.Key;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.lobby.LobbyIdentities;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import org.jetbrains.annotations.Nullable;

/**
 * Contributes the {@code titan:lobbyswitcher} clock to the platform-wide lobby items, but only when
 * the flag is on and this lobby knows its own identity: the hotbar builds its item maps once, so a
 * clock that cannot work must not exist at all.
 */
@Factory
@Profile(LobbySwitcherModule.CLOUDNET)
final class LobbySwitcherItems {

    private static final String FEATURE_ID = "lobbyswitcher";
    private static final Key ITEM_KEY = Key.key("titan:lobbyswitcher");
    private static final int HOTBAR_SLOT = 8;

    /** {@code null} - no bean - when the flag is off or the identity is unknown. */
    @Bean
    @Nullable
    LobbyItem lobbySwitcherItem(LobbySwitcherModule module, FeatureFlags flags, LobbyIdentities identities, LobbySwitcherMessages messages) {
        if (!flags.isActive(LobbySwitcherModule.FLAG) || identities.self().isEmpty()) {
            return null;
        }
        ItemStack clock = ItemStack.builder(Material.CLOCK).customName(messages.itemName(Locale.ENGLISH)).build();
        return new LobbyItem(FEATURE_ID, ITEM_KEY, clock, ItemSlot.hotbar(HOTBAR_SLOT), (player, event) -> module.open(player));
    }
}
