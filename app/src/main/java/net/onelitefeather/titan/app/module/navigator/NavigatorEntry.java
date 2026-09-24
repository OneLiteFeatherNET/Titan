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
package net.onelitefeather.titan.app.module.navigator;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;

/**
 * One destination shown in the lobby navigator.
 *
 * <p>Immutable data only, see {@code design.md}, decision 8: this record describes *what* a
 * navigator entry is - a slot, an icon, a display name and the CloudNet task name a click delivers
 * the player to - never *how* it is rendered or clicked. Building the shared navigator inventory
 * and reacting to a click on it is the navigator feature module's job, added in a later wave.
 *
 * @param slot        the slot this entry occupies, {@code 0}-{@code 8}, matching
 *                    {@link InventoryType#CHEST_1_ROW}
 * @param icon        the item shown in {@code slot}
 * @param displayName the name shown to the player
 * @param destination the CloudNet task name a click on this entry delivers the player to
 */
public record NavigatorEntry(int slot, ItemStack icon, Component displayName, String destination) {

    /**
     * @throws IllegalArgumentException if {@code slot} is outside {@code 0}-{@code 8}, or
     *                                  {@code destination} is blank
     * @throws NullPointerException     if {@code icon}, {@code displayName} or {@code destination}
     *                                  is {@code null}
     */
    public NavigatorEntry {
        if (slot < 0 || slot > 8) {
            throw new IllegalArgumentException("slot must be between 0 and 8 (CHEST_1_ROW), was " + slot);
        }
        Objects.requireNonNull(icon, "icon must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        if (destination.isBlank()) {
            throw new IllegalArgumentException("destination must not be blank");
        }
    }
}
