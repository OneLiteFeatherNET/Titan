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

import java.util.Arrays;
import java.util.List;
import net.onelitefeather.titan.core.portal.ServiceCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PickerSortTest {

    // Named for the order they are checked in, so a failure says which service sorted where.
    private static List<String> order(PickerSort sort, ServiceCount... services) {
        return Arrays.stream(services).sorted(sort.comparator()).map(ServiceCount::name).toList();
    }

    @DisplayName("Name sorts the services A to Z")
    @Test
    void nameSortsAlphabetically() {
        assertEquals(List.of("Lobby-1", "Lobby-2", "Lobby-3"), order(PickerSort.NAME, new ServiceCount("Lobby-2", 0, 50), new ServiceCount("Lobby-1", 0, 50), new ServiceCount("Lobby-3", 0, 50)), "the standard order is by name");
    }

    @DisplayName("Most players puts the fullest service first")
    @Test
    void mostPlayersSortsByOnlineDescending() {
        assertEquals(List.of("B", "A", "C"), order(PickerSort.MOST_PLAYERS, new ServiceCount("A", 12, 100), new ServiceCount("B", 40, 100), new ServiceCount("C", 0, 100)), "B has the most players, then A, then C");
    }

    @DisplayName("Most players breaks a tie by name")
    @Test
    void mostPlayersTieGoesToTheName() {
        assertEquals(List.of("ElytraRace-1", "ElytraRace-2"), order(PickerSort.MOST_PLAYERS, new ServiceCount("ElytraRace-2", 12, 50), new ServiceCount("ElytraRace-1", 12, 50)), "equal players sort by name");
    }

    @DisplayName("Free slots puts the most free places first")
    @Test
    void mostFreeSortsByFreePlacesDescending() {
        assertEquals(List.of("A", "B"), order(PickerSort.MOST_FREE, new ServiceCount("B", 40, 50), new ServiceCount("A", 12, 100)), "A has 88 free places, B has 10");
    }

    @DisplayName("Free slots puts services without an announced limit last")
    @Test
    void mostFreeSortsServicesWithoutLimitLast() {
        assertEquals(List.of("A", "B", "C"), order(PickerSort.MOST_FREE, new ServiceCount("C", 3, 0), new ServiceCount("B", 40, 50), new ServiceCount("A", 12, 100)), "the service without a limit comes after every announced one");
    }

    @DisplayName("Free slots breaks a tie by name")
    @Test
    void mostFreeTieGoesToTheName() {
        assertEquals(List.of("A", "B"), order(PickerSort.MOST_FREE, new ServiceCount("B", 10, 50), new ServiceCount("A", 10, 50)), "equal free places sort by name");
    }

    @DisplayName("Status lists joinable services first, then not ready, then full")
    @Test
    void statusRanksJoinableNotReadyFull() {
        assertEquals(List.of("A", "C", "B"), order(PickerSort.STATUS, new ServiceCount("B", 10, 10), new ServiceCount("C", 3, 0), new ServiceCount("A", 5, 50)), "joinable, then not ready, then full");
    }

    @DisplayName("Status breaks a tie by name")
    @Test
    void statusTieGoesToTheName() {
        assertEquals(List.of("A", "B"), order(PickerSort.STATUS, new ServiceCount("B", 5, 50), new ServiceCount("A", 9, 50)), "two joinable services sort by name");
    }

    @DisplayName("The sort order cycles from status back to name, and four clicks return to the start")
    @Test
    void nextCyclesThroughAllOrders() {
        assertEquals(PickerSort.MOST_PLAYERS, PickerSort.NAME.next(), "the second order follows the first");
        assertEquals(PickerSort.STATUS, PickerSort.MOST_FREE.next(), "status comes after free slots");
        assertEquals(PickerSort.NAME, PickerSort.STATUS.next(), "after status the order starts again at name");
        assertEquals(PickerSort.NAME, PickerSort.NAME.next().next().next().next(), "four clicks return to name");
    }
}
