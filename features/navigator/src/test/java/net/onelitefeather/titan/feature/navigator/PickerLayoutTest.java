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

    @DisplayName("The navigation row holds the sort item in its third slot")
    @Test
    void sortItemSitsInTheNavigationRow() {
        PickerLayout paged = PickerLayout.of(100);

        assertEquals(45, paged.previousSlot(), "the previous arrow is the first slot of the navigation row");
        assertEquals(47, paged.sortSlot(), "the sort item is the third slot of the navigation row");
        assertEquals(49, paged.indicatorSlot(), "the page indicator is the middle slot");
        assertEquals(53, paged.nextSlot(), "the next arrow is the last slot");
    }

    @DisplayName("Without pages the navigation row is the bottom row of the smaller inventory")
    @Test
    void navigationRowFollowsTheRows() {
        PickerLayout small = PickerLayout.of(10);

        assertEquals(18, small.previousSlot(), "the navigation row of three rows starts after 18 slots");
        assertEquals(20, small.sortSlot(), "the sort item is still the third slot of that row");
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

    @DisplayName("The page indicator shows only when there is more than one page")
    @Test
    void indicatorOnlyWhenPaged() {
        assertFalse(PickerLayout.of(45).indicatorShown(), "one page needs no indicator");
        assertTrue(PickerLayout.of(46).indicatorShown(), "two pages show the indicator");
    }
}
