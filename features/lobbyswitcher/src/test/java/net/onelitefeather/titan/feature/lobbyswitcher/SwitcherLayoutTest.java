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
package net.onelitefeather.titan.feature.lobbyswitcher;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class SwitcherLayoutTest {

    private record Case(int entries, int rows, int pages) {
    }

    @DisplayName("Rows include the navigation row; up to 45 entries need one to five content rows")
    @TestFactory
    Stream<DynamicTest> rowsAndPages() {
        return Stream.of(new Case(0, 2, 1), new Case(1, 2, 1), new Case(9, 2, 1), new Case(10, 3, 1), new Case(45, 6, 1), new Case(46, 6, 2), new Case(90, 6, 2), new Case(91, 6, 3), new Case(100, 6, 3)).map(c -> DynamicTest.dynamicTest(c.entries() + " entries", () -> {
            SwitcherLayout layout = SwitcherLayout.of(c.entries());
            Assertions.assertEquals(c.rows(), layout.rows(), "rows for " + c.entries());
            Assertions.assertEquals(c.pages(), layout.pages(), "pages for " + c.entries());
        }));
    }

    @DisplayName("The navigation row is the last row, also for a single page")
    @Test
    void navigationRowIsTheLastRow() {
        Assertions.assertEquals(9, SwitcherLayout.of(1).navigationStart(), "one lobby: one content row above the navigation");
        Assertions.assertEquals(45, SwitcherLayout.of(90).navigationStart(), "paged: five content rows above the navigation");
    }

    @DisplayName("The navigation slots sit at the start of the navigation row, offset 0, 3, 4 and 5")
    @Test
    void navigationSlotsAreRelativeToTheRow() {
        SwitcherLayout single = SwitcherLayout.of(3);
        Assertions.assertEquals(List.of(9, 12, 13, 14), List.of(single.closeSlot(), single.previousSlot(), single.indicatorSlot(), single.nextSlot()), "one page: close at 9, arrows and indicator at 12, 13, 14");

        SwitcherLayout paged = SwitcherLayout.of(100);
        Assertions.assertEquals(List.of(45, 48, 49, 50), List.of(paged.closeSlot(), paged.previousSlot(), paged.indicatorSlot(), paged.nextSlot()), "paged: close at 45, previous at 48, indicator at 49, next at 50");
    }

    @DisplayName("Paging is only needed above 45 entries")
    @Test
    void pagingOnlyAboveAPage() {
        Assertions.assertFalse(SwitcherLayout.of(45).paged(), "45 entries fit one page");
        Assertions.assertTrue(SwitcherLayout.of(46).paged(), "46 entries need a second page");
    }

    @DisplayName("A page holds 45 entries in name order; the last page holds the rest")
    @Test
    void pageSlices() {
        List<Integer> entries = IntStream.range(0, 100).boxed().toList();
        SwitcherLayout layout = SwitcherLayout.of(entries.size());

        Assertions.assertEquals(entries.subList(0, 45), layout.page(entries, 0));
        Assertions.assertEquals(entries.subList(45, 90), layout.page(entries, 1));
        Assertions.assertEquals(entries.subList(90, 100), layout.page(entries, 2));
    }

    @DisplayName("Without paging the single page holds every entry")
    @Test
    void singlePageHoldsAll() {
        List<Integer> entries = IntStream.range(0, 45).boxed().toList();

        Assertions.assertEquals(entries, SwitcherLayout.of(45).page(entries, 0));
    }
}
