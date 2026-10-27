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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minestom.server.inventory.AbstractInventory;
import net.theevilreaper.aves.inventory.CustomInventory;
import net.theevilreaper.aves.inventory.holder.InventoryHolderImpl;

/** The radio windows that are open, one per player. Only the tick thread touches them. */
final class RadioViews {

    private final Map<UUID, RadioWindow> windows = new HashMap<>();

    /**
     * Puts {@code window} in place of the player's window and returns the one it replaced, if any.
     */
    RadioWindow put(UUID player, RadioWindow window) {
        return this.windows.put(player, window);
    }

    /** Takes the player's window out, so a close of it is no longer the radio's concern. */
    Optional<RadioWindow> remove(UUID player) {
        return Optional.ofNullable(this.windows.remove(player));
    }

    /** Which open window {@code inventory} is, or nothing when it is not one of the radio's. */
    Optional<RadioWindow> ownerOf(AbstractInventory inventory) {
        if (inventory instanceof CustomInventory custom && custom.getHolder() instanceof InventoryHolderImpl holder) {
            Object builder = holder.inventoryBuilder();
            return this.windows.values().stream().filter(window -> window == builder).findFirst();
        }
        return Optional.empty();
    }

    /** The window the player has open, if the radio opened one. */
    Optional<RadioWindow> find(UUID player) {
        return Optional.ofNullable(this.windows.get(player));
    }

    Collection<Map.Entry<UUID, RadioWindow>> entries() {
        return this.windows.entrySet();
    }

    void clear() {
        this.windows.clear();
    }
}
