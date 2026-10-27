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
package net.onelitefeather.titan.feature.radio;

/**
 * Pure paging math for the radio menu: a 54-slot window whose last nine slots
 * are a fixed control row, so turning a page never moves the controls.
 */
public final class PageLayout {

    /** Content slots per page, indices 0 to 44 of the window. */
    public static final int PAGE_SIZE = 45;

    /** First slot of the fixed control row. */
    public static final int CONTROL_ROW_FIRST = 45;

    /** Slots in the whole window (six rows). */
    public static final int SLOT_COUNT = 54;

    private PageLayout() {
    }

    /** Number of pages for the items; an empty category still shows one page. */
    public static int pageCount(int items) {
        return Math.max(1, (items + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    /** Slot inside the content area of the page that holds the item at {@code index}. */
    public static int slotOf(int index) {
        return index % PAGE_SIZE;
    }

    /** 1-based page that holds the item at {@code index}. */
    public static int pageOf(int index) {
        return index / PAGE_SIZE + 1;
    }

    /** Keeps {@code page} between the first page and the last page for {@code items}. */
    public static int clampPage(int page, int items) {
        return Math.clamp(page, 1, pageCount(items));
    }
}
