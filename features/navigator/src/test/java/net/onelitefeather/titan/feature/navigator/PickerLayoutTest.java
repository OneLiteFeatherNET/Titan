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

import net.onelitefeather.titan.core.paging.Pagination;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PickerLayoutTest {

    @DisplayName("The rows follow the services, with one navigation row at the bottom")
    @Test
    void rowsFollowTheServices() {
        assertEquals(2, PickerLayout.of(0).rows(), "no service: one explanation row and the navigation");
        assertEquals(2, PickerLayout.of(1).rows(), "one service fits one row");
        assertEquals(2, PickerLayout.of(9).rows(), "nine services fit one row");
        assertEquals(3, PickerLayout.of(10).rows(), "ten services need two rows");
        assertEquals(6, PickerLayout.of(45).rows(), "45 services fill five rows and the navigation");
        assertEquals(6, PickerLayout.of(46).rows(), "a second page keeps six rows");
    }

    @DisplayName("Pages hold 45 services; a list of 45 or fewer has one page")
    @Test
    void pagesFollowTheServices() {
        assertEquals(1, PickerLayout.of(45).pages(), "45 services fit one page");
        assertEquals(2, PickerLayout.of(46).pages(), "46 services need two pages");
        assertEquals(2, PickerLayout.of(90).pages(), "90 services fill two pages");
    }

    @DisplayName("A page is five rows of services, with the sixth row kept for navigation")
    @Test
    void pageSizeIsFiveContentRows() {
        assertEquals(5 * ServerPicker.COLUMNS, Pagination.PAGE_SIZE, "a page holds five content rows of 9 services (45 entries); the sixth row is the navigation row");
    }

    @DisplayName("The navigation row holds the arrows at its ends and the sort item in its middle")
    @Test
    void navigationRowHoldsArrowsAndSortItem() {
        PickerLayout paged = PickerLayout.of(100);

        assertEquals(45, paged.previousSlot(), "the previous arrow is the first slot of the navigation row");
        assertEquals(49, paged.sortSlot(), "the sort item is the middle slot of the navigation row, navigation start + 4");
        assertEquals(53, paged.nextSlot(), "the next arrow is the last slot of the navigation row, navigation start + 8");
    }

    @DisplayName("Without pages the navigation row is the bottom row of the smaller inventory, with the same layout")
    @Test
    void navigationRowFollowsTheRows() {
        PickerLayout small = PickerLayout.of(10);

        assertEquals(18, small.previousSlot(), "the navigation row of three rows starts after 18 slots");
        assertEquals(22, small.sortSlot(), "the sort item is the middle slot of that row");
        assertEquals(26, small.nextSlot(), "the last slot of that row is the next arrow's place");
    }

    @DisplayName("The previous arrow shows from the second page on")
    @Test
    void previousArrowFromSecondPage() {
        PickerLayout paged = PickerLayout.of(100);

        assertFalse(paged.previousShown(0), "no page before the first");
        assertTrue(paged.previousShown(1), "the second page has one before it");
    }

    @DisplayName("The next arrow shows while a page follows")
    @Test
    void nextArrowWhilePageFollows() {
        PickerLayout paged = PickerLayout.of(100);

        assertTrue(paged.nextShown(0), "the first of three pages has one after it");
        assertTrue(paged.nextShown(1), "the middle page has one after it");
        assertFalse(paged.nextShown(2), "the last page has none after it");
    }

    @DisplayName("A single entry is centred in the middle column of its row")
    @Test
    void singleEntryIsCentred() {
        assertEquals(4, PickerLayout.serviceSlot(0, 1), "one entry sits in column four, the middle of nine columns");
    }

    @DisplayName("Two entries are centred in columns three and four")
    @Test
    void twoEntriesAreCentred() {
        assertEquals(3, PickerLayout.serviceSlot(0, 2), "the first of two entries sits in column three: (9 - 2) / 2 = 3");
        assertEquals(4, PickerLayout.serviceSlot(1, 2), "the second of two entries sits in column four");
    }

    @DisplayName("Eight entries start in column zero, so the spare column is the last one")
    @Test
    void eightEntriesLeaveTheLastColumnFree() {
        assertEquals(0, PickerLayout.serviceSlot(0, 8), "(9 - 8) / 2 rounds down to zero: the first entry is in column zero");
        assertEquals(7, PickerLayout.serviceSlot(7, 8), "the eighth entry is in column seven, column eight stays free");
    }

    @DisplayName("A row of nine entries is full and not shifted")
    @Test
    void fullRowIsNotShifted() {
        assertEquals(0, PickerLayout.serviceSlot(0, 9), "the first entry of a full row is in column zero");
        assertEquals(8, PickerLayout.serviceSlot(8, 9), "the last entry of a full row is in column eight");
    }

    @DisplayName("A short second row is centred, the first row stays full")
    @Test
    void shortSecondRowIsCentred() {
        assertEquals(8, PickerLayout.serviceSlot(8, 10), "the ninth entry is still the last one of the full first row");
        assertEquals(13, PickerLayout.serviceSlot(9, 10), "the tenth entry is alone in the second row, column four: 9 + 4");
    }

    @DisplayName("Forty-five entries fill five full rows, so every entry keeps its index as its slot")
    @Test
    void fortyFiveEntriesKeepTheirIndex() {
        for (int index = 0; index < 45; index++) {
            assertEquals(index, PickerLayout.serviceSlot(index, 45), "entry " + index + " of a full page keeps its slot");
        }
    }

    @DisplayName("The last row of a page is centred on that page's own count")
    @Test
    void lastPageRowIsCentredOnThePage() {
        assertEquals(11, PickerLayout.serviceSlot(9, 14), "a page of 14 has five entries in its second row: (9 - 5) / 2 = 2, so 9 + 2");
    }
}
