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

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.core.module.item.LobbyItems;

/**
 * The hotbar column's job in miniature: places the hotbar items on equip. This column must not
 * depend on {@code features/hotbar}, so its tests bring their own.
 */
final class PlacingLobbyItems implements LobbyItems {

    private final List<LobbyItem> items;

    PlacingLobbyItems(List<LobbyItem> items) {
        this.items = items;
    }

    @Override
    public void equip(Player player) {
        player.getInventory().clear();
        for (LobbyItem item : this.items) {
            if (item.placement() instanceof ItemSlot.Hotbar hotbar) {
                player.getInventory().setItemStack(hotbar.slot(), item.stackFor(Objects.requireNonNullElse(player.getLocale(), Locale.ENGLISH)));
            }
        }
    }

    @Override
    public ItemStack stack(String key) {
        return this.items.stream().filter(item -> item.key().asString().equals(key)).findFirst().orElseThrow().itemStack();
    }
}
