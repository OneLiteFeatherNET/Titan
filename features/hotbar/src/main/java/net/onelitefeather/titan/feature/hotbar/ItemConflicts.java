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
package net.onelitefeather.titan.feature.hotbar;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;

/**
 * Pure conflict detection for {@link LobbyItem} beans, used by {@link LobbyItems} on
 * construction: kept free of {@link LobbyItems} and anything needing a running server.
 *
 * <p>The first item to claim a key or a fixed placement wins; a later one claiming the same is a
 * conflict, and {@link ItemSlot.Unplaced} never conflicts.
 */
final class ItemConflicts {

    private ItemConflicts() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * @throws IllegalStateException naming both items; the conflict is also a span event on the
     *                               current span, so the startup trace shows which items clashed
     */
    static void check(List<LobbyItem> items) {
        Map<String, LobbyItem> claimedKeys = new LinkedHashMap<>();
        Map<ItemSlot, LobbyItem> claimedSlots = new LinkedHashMap<>();
        for (LobbyItem item : items) {
            String key = item.key().asString();
            LobbyItem previousKey = claimedKeys.putIfAbsent(key, item);
            if (previousKey != null) {
                throw conflict("key", item, "Items " + describe(previousKey) + " and " + describe(item) + " both use key '" + key + "'");
            }
            if (!(item.placement() instanceof ItemSlot.Unplaced)) {
                LobbyItem previousSlot = claimedSlots.putIfAbsent(item.placement(), item);
                if (previousSlot != null) {
                    throw conflict("slot", item, "Items " + describe(previousSlot) + " and " + describe(item) + " both claim " + describeSlot(item.placement()));
                }
            }
        }
    }

    private static IllegalStateException conflict(String kind, LobbyItem item, String message) {
        Span.current().addEvent(HotbarTelemetry.CONFLICT_EVENT, Attributes.of(HotbarTelemetry.CONFLICT_KIND, kind, HotbarTelemetry.ITEM, item.key().asString()));
        return new IllegalStateException(message);
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
