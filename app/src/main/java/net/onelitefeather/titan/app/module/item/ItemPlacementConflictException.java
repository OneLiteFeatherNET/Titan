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

/**
 * Thrown by {@link ItemRegistry#validate()} when two modules registered an item for the same
 * {@link ItemSlot}. The message names the contested placement and both modules, so an operator
 * does not have to read code to find the conflict.
 */
public final class ItemPlacementConflictException extends RuntimeException {

    ItemPlacementConflictException(SlotConflictDetector.Conflict conflict) {
        super("Modules '" + conflict.firstModuleId() + "' and '" + conflict.secondModuleId() + "' both claim " + describe(conflict.placement()));
    }

    private static String describe(ItemSlot placement) {
        return switch (placement) {
            case ItemSlot.Hotbar hotbar -> "hotbar slot " + hotbar.slot();
            case ItemSlot.Equipment equipment -> "equipment slot " + equipment.slot();
            // Unreachable: SlotConflictDetector never reports a conflict for an unplaced item.
            case ItemSlot.Unplaced ignored -> "no fixed placement";
        };
    }
}
