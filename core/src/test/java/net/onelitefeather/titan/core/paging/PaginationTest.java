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
package net.onelitefeather.titan.core.paging;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaginationTest {

    private static List<Integer> entries(int count) {
        return IntStream.range(0, count).boxed().toList();
    }

    @DisplayName("Pages hold 45 entries, and an empty list still has one page")
    @Test
    void pageCountFollowsTheEntryCount() {
        assertEquals(1, Pagination.pageCount(0), "an empty list has one page");
        assertEquals(1, Pagination.pageCount(1), "one entry needs one page");
        assertEquals(1, Pagination.pageCount(45), "45 entries fill one page exactly");
        assertEquals(2, Pagination.pageCount(46), "one entry more needs a second page");
        assertEquals(2, Pagination.pageCount(90), "90 entries fill two pages exactly");
    }

    @DisplayName("A full page holds the first 45 entries")
    @Test
    void fullPageHoldsFortyFive() {
        assertEquals(entries(45), Pagination.slice(entries(100), 0), "page 0 holds entries 0 to 44");
        assertEquals(entries(90).subList(45, 90), Pagination.slice(entries(90), 1), "page 1 holds entries 45 to 89");
    }

    @DisplayName("The last page holds the entries that remain")
    @Test
    void lastPageHoldsTheRemainder() {
        assertEquals(entries(100).subList(90, 100), Pagination.slice(entries(100), 2), "page 2 holds the last ten entries");
    }

    @DisplayName("A page beyond the last one holds nothing")
    @Test
    void pageBeyondTheEndIsEmpty() {
        assertEquals(List.of(), Pagination.slice(entries(100), 3), "there is no page 3 of 100 entries");
    }

    @DisplayName("A negative page holds nothing")
    @Test
    void negativePageIsEmpty() {
        assertEquals(List.of(), Pagination.slice(entries(100), -1), "there is no page -1");
    }

    @DisplayName("A page number is clamped to the pages that exist")
    @Test
    void clampKeepsThePageInRange() {
        assertEquals(0, Pagination.clamp(-3, 100), "below the first page is the first page");
        assertEquals(2, Pagination.clamp(7, 100), "beyond the last page is the last page");
        assertEquals(1, Pagination.clamp(1, 100), "a page that exists stays as it is");
        assertEquals(0, Pagination.clamp(4, 0), "an empty list has only page 0");
    }
}
