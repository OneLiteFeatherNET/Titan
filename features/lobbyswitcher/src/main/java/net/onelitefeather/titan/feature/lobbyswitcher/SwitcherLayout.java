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
import net.onelitefeather.titan.core.paging.Pagination;

/**
 * How many rows and pages a lobby list needs. The last row is always the navigation row, so
 * {@code rows} counts it too; the content above it is one to five rows, and a list beyond
 * {@link Pagination#PAGE_SIZE} entries pages with five content rows on every page.
 */
record SwitcherLayout(int rows, int pages) {

    static final int SLOTS_PER_ROW = 9;
    static final int MAX_ROWS = 6;

    static SwitcherLayout of(int entries) {
        if (entries > Pagination.PAGE_SIZE) {
            return new SwitcherLayout(MAX_ROWS, Pagination.pageCount(entries));
        }
        return new SwitcherLayout(Math.max(1, Math.ceilDiv(entries, SLOTS_PER_ROW)) + 1, 1);
    }

    /** The first slot of the navigation row. */
    int navigationStart() {
        return (this.rows - 1) * SLOTS_PER_ROW;
    }

    /** Closes the list; always the first slot of the navigation row, whatever the page. */
    int closeSlot() {
        return navigationStart();
    }

    /** The previous-page arrow, three slots after the close button. */
    int previousSlot() {
        return navigationStart() + 3;
    }

    /** The page indicator, in the centre of the navigation row. */
    int indicatorSlot() {
        return navigationStart() + 4;
    }

    /** The next-page arrow, five slots after the close button. */
    int nextSlot() {
        return navigationStart() + 5;
    }

    boolean paged() {
        return this.pages > 1;
    }

    /** The entries shown on {@code page}, counted from zero. */
    <T> List<T> page(List<T> entries, int page) {
        if (!paged()) {
            return entries;
        }
        return Pagination.slice(entries, page);
    }
}
