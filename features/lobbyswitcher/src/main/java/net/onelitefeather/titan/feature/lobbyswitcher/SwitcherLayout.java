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
 * How many rows and pages a lobby list needs. Up to {@value #MAX_ROWS} rows hold the list as it
 * is; beyond that every page keeps five content rows and the sixth row is the navigation.
 */
record SwitcherLayout(int rows, int pages) {

    static final int SLOTS_PER_ROW = 9;
    static final int MAX_ROWS = 6;
    static final int MAX_ENTRIES = SLOTS_PER_ROW * MAX_ROWS;
    static final int PAGE_ENTRIES = Pagination.PAGE_SIZE;
    static final int PREVIOUS_SLOT = PAGE_ENTRIES;
    static final int INDICATOR_SLOT = PAGE_ENTRIES + SLOTS_PER_ROW / 2;
    static final int NEXT_SLOT = MAX_ENTRIES - 1;

    static SwitcherLayout of(int entries) {
        if (entries > MAX_ENTRIES) {
            return new SwitcherLayout(MAX_ROWS, Pagination.pageCount(entries));
        }
        return new SwitcherLayout(Math.max(1, Math.ceilDiv(entries, SLOTS_PER_ROW)), 1);
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
