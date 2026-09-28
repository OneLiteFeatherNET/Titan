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
package net.onelitefeather.titan.core.module.item;

import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;

/**
 * The platform-wide home for every {@link LobbyItem} a column contributes: {@link #equip(Player)}
 * gives a player the standard loadout, {@link #stack(String)} hands back the stamped stack for an
 * item with no fixed placement (e.g. the elytra's firework, given out and taken back by its own
 * feature). The implementation, {@code HotbarLobbyItems}, lives in {@code features/hotbar} (in
 * this wave, temporarily in {@code :app}) - a column depends only on this interface (DIP, ISP: a
 * column never sees {@code itemCount()} or how items are collected).
 */
public interface LobbyItems {

    /** Clears {@code player}'s inventory and sets every item that has a fixed placement. */
    void equip(Player player);

    /**
     * @param key a {@link LobbyItem}'s key, e.g. {@code "titan:firework"}
     * @throws IllegalArgumentException if no item with that key exists
     */
    ItemStack stack(String key);
}
