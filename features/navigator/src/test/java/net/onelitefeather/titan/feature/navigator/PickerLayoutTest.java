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

    @DisplayName("Six rows with pages: the back button at navigation start, the arrows beside the sort item")
    @Test
    void navigationRowOfAPagedList() {
        PickerLayout paged = PickerLayout.of(100);

        assertEquals(45, paged.backSlot(), "the back button is the first slot of the navigation row, navigation start + 0");
        assertEquals(48, paged.previousSlot(), "the previous arrow is navigation start + 3, next to the sort item");
        assertEquals(49, paged.sortSlot(), "the sort item is the middle slot of the navigation row, navigation start + 4");
        assertEquals(50, paged.nextSlot(), "the next arrow is navigation start + 5, next to the sort item");
    }

    @DisplayName("Three rows without pages: the back button and the arrows keep the same offsets from the row start")
    @Test
    void navigationRowOfThreeRows() {
        PickerLayout small = PickerLayout.of(10);

        assertEquals(3, small.rows(), "ten services need two content rows and the navigation row");
        assertEquals(18, small.backSlot(), "the back button is the first slot of the navigation row of three rows");
        assertEquals(21, small.previousSlot(), "the previous arrow is navigation start + 3");
        assertEquals(22, small.sortSlot(), "the sort item is the middle slot of that row");
        assertEquals(23, small.nextSlot(), "the next arrow is navigation start + 5");
    }

    @DisplayName("Two rows with one content row: the back button in slot nine, the sort item in slot thirteen")
    @Test
    void navigationRowOfTwoRows() {
        PickerLayout single = PickerLayout.of(1);

        assertEquals(2, single.rows(), "one service needs one content row and the navigation row");
        assertEquals(9, single.backSlot(), "the back button is the first slot of the navigation row of two rows");
        assertEquals(12, single.previousSlot(), "the previous arrow is navigation start + 3");
        assertEquals(13, single.sortSlot(), "the sort item is the middle slot of the navigation row");
        assertEquals(14, single.nextSlot(), "the next arrow is navigation start + 5");
    }

    @DisplayName("An empty list keeps the back button in the navigation row")
    @Test
    void emptyListKeepsTheBackButton() {
        PickerLayout empty = PickerLayout.of(0);

        assertEquals(9, empty.backSlot(), "the back button is in the navigation row of the explanation's two rows");
        assertEquals(13, empty.sortSlot(), "the sort item is still in the middle of that row");
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
