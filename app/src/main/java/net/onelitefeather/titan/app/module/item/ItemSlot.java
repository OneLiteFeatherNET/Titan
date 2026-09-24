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

import java.util.Objects;
import net.minestom.server.entity.EquipmentSlot;

/**
 * Where a {@link LobbyItem} lives, if anywhere.
 *
 * <p>Sealed to exactly the three shapes the {@code lobby-hotbar} spec allows: a fixed hotbar slot,
 * a fixed equipment slot, or no fixed place at all. {@link ItemRegistry#validate()} only checks the
 * first two for conflicts - two modules may both hand out an unplaced item (the elytra module's
 * firework, for instance) without ever colliding, because neither one reserves a place the other
 * could also claim.
 */
public sealed interface ItemSlot {

    /** The lowest hotbar slot a {@link Hotbar} placement may name. */
    int MIN_HOTBAR_SLOT = 0;

    /** The highest hotbar slot a {@link Hotbar} placement may name. */
    int MAX_HOTBAR_SLOT = 8;

    /**
     * @param slot the hotbar slot, {@value #MIN_HOTBAR_SLOT}-{@value #MAX_HOTBAR_SLOT}
     * @return a placement pinned to that hotbar slot
     * @throws IllegalArgumentException if {@code slot} is outside the hotbar
     */
    static ItemSlot hotbar(int slot) {
        return new Hotbar(slot);
    }

    /**
     * @param slot the equipment slot, e.g. {@link EquipmentSlot#CHESTPLATE}
     * @return a placement pinned to that equipment slot
     */
    static ItemSlot equipment(EquipmentSlot slot) {
        return new Equipment(slot);
    }

    /**
     * @return a placement for an item with no fixed place - the owning module hands it out and
     *         takes it back itself, and it is exempt from conflict checking
     */
    static ItemSlot unplaced() {
        return new Unplaced();
    }

    /** A fixed hotbar slot, {@value ItemSlot#MIN_HOTBAR_SLOT}-{@value ItemSlot#MAX_HOTBAR_SLOT}. */
    record Hotbar(int slot) implements ItemSlot {

        public Hotbar {
            if (slot < MIN_HOTBAR_SLOT || slot > MAX_HOTBAR_SLOT) {
                throw new IllegalArgumentException("Hotbar slot must be between " + MIN_HOTBAR_SLOT + " and " + MAX_HOTBAR_SLOT + ", was " + slot);
            }
        }
    }

    /**
     * A fixed equipment slot, e.g. {@link EquipmentSlot#CHESTPLATE} or
     * {@link EquipmentSlot#OFF_HAND}.
     */
    record Equipment(EquipmentSlot slot) implements ItemSlot {

        public Equipment {
            Objects.requireNonNull(slot, "slot");
        }
    }

    /** No fixed place; the owning module gives and removes the item itself. */
    record Unplaced() implements ItemSlot {
    }
}
