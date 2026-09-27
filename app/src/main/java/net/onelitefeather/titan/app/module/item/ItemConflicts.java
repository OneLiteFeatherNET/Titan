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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure conflict detection for {@link LobbyItem} beans, used by {@link LobbyItems} on construction
 * (see {@code openspec/changes/dissolve-module-platform/design.md}, decision 2).
 *
 * <p>Mirrors what {@code DuplicateItemKeyDetector} and {@code SlotConflictDetector} did for the
 * old {@code ItemRegistry} - kept free of {@link LobbyItems} and everything else that needs a
 * running server, so the rule itself - "first item wins, a later item claiming the same key or the
 * same fixed placement is a conflict, {@link ItemSlot.Unplaced} never conflicts" - is testable as
 * plain data in, exception out. Two conflict kinds share one pass over {@code items} because both
 * are checked in registration order and a `LobbyItems` build only needs to abort on the first one
 * found (YAGNI - a second detector class would just duplicate this loop).
 */
final class ItemConflicts {

    private ItemConflicts() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * @param items every item a {@link LobbyItems} bean is about to be built from, in the order
     *              Avaje Inject's list injection handed them
     * @throws IllegalStateException if two items claim the same {@link LobbyItem#key()} or the
     *                               same fixed {@link ItemSlot}; the message names the contested
     *                               key or slot and both items
     */
    static void check(List<LobbyItem> items) {
        Map<String, LobbyItem> claimedKeys = new LinkedHashMap<>();
        Map<ItemSlot, LobbyItem> claimedSlots = new LinkedHashMap<>();
        for (LobbyItem item : items) {
            String key = item.key().asString();
            LobbyItem previousKey = claimedKeys.putIfAbsent(key, item);
            if (previousKey != null) {
                throw new IllegalStateException("Items " + describe(previousKey) + " and " + describe(item) + " both use key '" + key + "'");
            }
            if (!(item.placement() instanceof ItemSlot.Unplaced)) {
                LobbyItem previousSlot = claimedSlots.putIfAbsent(item.placement(), item);
                if (previousSlot != null) {
                    throw new IllegalStateException("Items " + describe(previousSlot) + " and " + describe(item) + " both claim " + describeSlot(item.placement()));
                }
            }
        }
    }

    private static String describe(LobbyItem item) {
        return "'" + item.key().asString() + "' (" + item.itemStack().material().key().asString() + ")";
    }

    private static String describeSlot(ItemSlot placement) {
        return switch (placement) {
            case ItemSlot.Hotbar hotbar -> "hotbar slot " + hotbar.slot();
            case ItemSlot.Equipment equipment -> "equipment slot " + equipment.slot();
            // Unreachable: an Unplaced item never claims claimedSlots above.
            case ItemSlot.Unplaced ignored -> "no fixed placement";
        };
    }
}
