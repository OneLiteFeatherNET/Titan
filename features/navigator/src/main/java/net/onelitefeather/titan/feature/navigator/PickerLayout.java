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
 * How many rows and pages the server picker needs, where its navigation row sits and where each
 * entry goes. The bottom row is always the navigation row: the previous arrow in its first slot,
 * the sort item in its middle slot and the next arrow in its last. A list that fits one page has
 * only the rows its services need above it.
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

    int previousSlot() {
        return navigationStart();
    }

    int sortSlot() {
        return navigationStart() + ServerPicker.COLUMNS / 2;
    }

    int nextSlot() {
        return navigationStart() + ServerPicker.COLUMNS - 1;
    }

    /**
     * The slot of entry {@code index} among the {@code count} entries of its page. A row that is
     * not full is centred: its entries start in column {@code (COLUMNS - n) / 2}, so one entry sits
     * in the middle column. Rendering and clicks both take the slot from here.
     */
    static int serviceSlot(int index, int count) {
        int row = index / ServerPicker.COLUMNS;
        int rowEntries = Math.min(ServerPicker.COLUMNS, count - row * ServerPicker.COLUMNS);
        int offset = (ServerPicker.COLUMNS - rowEntries) / 2;
        return row * ServerPicker.COLUMNS + offset + index % ServerPicker.COLUMNS;
    }

    /** The previous arrow shows on every page after the first, counted from zero. */
    boolean previousShown(int page) {
        return page > 0;
    }

    /** The next arrow shows while a page follows {@code page}. */
    boolean nextShown(int page) {
        return page < this.pages - 1;
    }

    private int navigationStart() {
        return (this.rows - 1) * ServerPicker.COLUMNS;
    }
}
