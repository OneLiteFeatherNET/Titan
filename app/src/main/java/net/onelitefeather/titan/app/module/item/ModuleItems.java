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
package net.onelitefeather.titan.app.module.item;

import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;

/**
 * A module's own view of the platform-wide {@link ItemRegistry}, obtained through
 * {@code ModuleContext.items()}.
 *
 * <p>An item registered through this interface disappears again as soon as the owning module is
 * disabled - a module never has to remember what it registered or unregister it by hand, mirroring
 * {@code ModuleCommands} and {@code ModuleTasks}.
 */
public interface ModuleItems {

    /**
     * Registers {@code item} with the platform: {@link ItemRegistry} stamps its identity tag onto
     * the stack and, if the item has a fixed {@link ItemSlot}, considers that placement claimed
     * once
     * {@link ItemRegistry#validate()} runs. The item disappears again when the owning module is
     * disabled.
     *
     * @param item the item to register
     * @return the stamped stack - a module that hands the item out itself (an unplaced item, for
     *         instance) gives the player this stack, not {@link LobbyItem#itemStack()}
     */
    ItemStack register(LobbyItem item);

    /**
     * Clears {@code player}'s inventory and sets every item every module has registered with a
     * fixed placement. Not limited to the calling module's own items - the standard lobby loadout
     * is platform-wide, which is why the spawn and respawn modules are the ones expected to call
     * it.
     *
     * @param player the player to equip
     */
    void equip(Player player);
}
