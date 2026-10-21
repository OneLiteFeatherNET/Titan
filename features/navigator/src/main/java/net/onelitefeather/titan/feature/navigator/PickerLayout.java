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

/**
 * How many rows and pages the server picker needs, and where its navigation row sits. The bottom
 * row is always the navigation row: the previous arrow, the sort item, the page indicator and the
 * next arrow. A list that fits one page has only the rows its services need above it.
 *
 * <p>The sort item stays in the third slot of that row, so a paged list and a single page put it in
 * the same place relative to the row.
 */
record PickerLayout(int rows, int pages) {

    private static final int MAX_ROWS = 6;
    private static final int MAX_CONTENT_ROWS = MAX_ROWS - 1;

    static PickerLayout of(int entries) {
        int pages = Pagination.pageCount(entries);
        if (pages > 1) {
            return new PickerLayout(MAX_ROWS, pages);
        }
        int contentRows = Math.clamp(Math.ceilDiv(entries, ServerPicker.COLUMNS), 1, MAX_CONTENT_ROWS);
        return new PickerLayout(contentRows + 1, 1);
    }

    boolean paged() {
        return this.pages > 1;
    }

    int previousSlot() {
        return navigationStart();
    }

    int sortSlot() {
        return navigationStart() + 2;
    }

    int indicatorSlot() {
        return navigationStart() + 4;
    }

    int nextSlot() {
        return navigationStart() + ServerPicker.COLUMNS - 1;
    }

    /** The previous arrow shows on every page after the first, counted from zero. */
    boolean previousShown(int page) {
        return page > 0;
    }

    /** The next arrow shows while a page follows {@code page}. */
    boolean nextShown(int page) {
        return page < this.pages - 1;
    }

    boolean indicatorShown() {
        return paged();
    }

    private int navigationStart() {
        return (this.rows - 1) * ServerPicker.COLUMNS;
    }
}
