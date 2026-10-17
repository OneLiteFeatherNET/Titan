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

    @DisplayName("Rows follow the entries up to six; more entries need pages of five content rows")
    @TestFactory
    Stream<DynamicTest> rowsAndPages() {
        return Stream.of(new Case(0, 1, 1), new Case(1, 1, 1), new Case(9, 1, 1), new Case(10, 2, 1), new Case(45, 5, 1), new Case(46, 6, 1), new Case(54, 6, 1), new Case(55, 6, 2), new Case(90, 6, 2), new Case(91, 6, 3), new Case(100, 6, 3)).map(c -> DynamicTest.dynamicTest(c.entries() + " entries", () -> {
            SwitcherLayout layout = SwitcherLayout.of(c.entries());
            Assertions.assertEquals(c.rows(), layout.rows(), "rows for " + c.entries());
            Assertions.assertEquals(c.pages(), layout.pages(), "pages for " + c.entries());
        }));
    }

    @DisplayName("Navigation exists only when there is more than one page")
    @Test
    void navigationOnlyWhenPaged() {
        Assertions.assertFalse(SwitcherLayout.of(54).paged(), "54 entries fit six rows");
        Assertions.assertTrue(SwitcherLayout.of(55).paged(), "55 entries need a second page");
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
        List<Integer> entries = IntStream.range(0, 54).boxed().toList();

        Assertions.assertEquals(entries, SwitcherLayout.of(54).page(entries, 0));
    }
}
