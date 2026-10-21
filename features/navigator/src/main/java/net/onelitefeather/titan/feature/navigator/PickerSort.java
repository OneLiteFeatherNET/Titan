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
package net.onelitefeather.titan.feature.navigator;

import java.util.Comparator;
import net.onelitefeather.titan.core.portal.ServiceCount;

/**
 * The orders of the server picker, in the order a click on its sort item steps through them. Every
 * order ends in the service name, so the same snapshot always lists the same way.
 */
enum PickerSort {

    NAME(NavigatorMessages.SORT_NAME, Comparator.comparing(ServiceCount::name)), MOST_PLAYERS(NavigatorMessages.SORT_MOST_PLAYERS, Comparator.comparingInt(ServiceCount::online).reversed().thenComparing(ServiceCount::name)), MOST_FREE(NavigatorMessages.SORT_MOST_FREE, Comparator.comparing(ServiceCount::limitAnnounced).reversed().thenComparing(Comparator.comparingInt(PickerSort::freePlaces).reversed()).thenComparing(ServiceCount::name)), STATUS(NavigatorMessages.SORT_STATUS, Comparator.comparingInt(PickerSort::statusRank).thenComparing(ServiceCount::name));

    private final String key;
    private final Comparator<ServiceCount> comparator;

    PickerSort(String key, Comparator<ServiceCount> comparator) {
        this.key = key;
        this.comparator = comparator;
    }

    /** The translation key of the order's name. */
    String key() {
        return this.key;
    }

    Comparator<ServiceCount> comparator() {
        return this.comparator;
    }

    /** The order the next click selects; the last one leads back to the first. */
    PickerSort next() {
        PickerSort[] orders = values();
        return orders[(ordinal() + 1) % orders.length];
    }

    // A service without an announced limit has no places to count; it sorts after every one that has.
    private static int freePlaces(ServiceCount service) {
        return service.limitAnnounced() ? service.max() - service.online() : 0;
    }

    private static int statusRank(ServiceCount service) {
        return ServerPicker.STATUS_ORDER.indexOf(ServerPicker.stateOf(service));
    }
}
