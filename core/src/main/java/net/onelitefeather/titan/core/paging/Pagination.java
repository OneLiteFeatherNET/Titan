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

/**
 * The page arithmetic of the menus that list more entries than one inventory holds: pages of
 * {@link #PAGE_SIZE} entries, counted from zero. Pure, so every menu pages the same way.
 */
public final class Pagination {

    /** Entries per page: five inventory rows, the sixth row stays free for the navigation. */
    public static final int PAGE_SIZE = 45;

    private Pagination() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /** The pages {@code entries} need; an empty list still has one, so a menu always shows one. */
    public static int pageCount(int entries) {
        return Math.max(1, Math.ceilDiv(entries, PAGE_SIZE));
    }

    /** The page number moved into the pages that exist for {@code entries}. */
    public static int clamp(int page, int entries) {
        return Math.clamp(page, 0, pageCount(entries) - 1);
    }

    /** The entries of {@code page}; a page that does not exist holds nothing. */
    public static <T> List<T> slice(List<T> entries, int page) {
        int from = page * PAGE_SIZE;
        if (page < 0 || from >= entries.size()) {
            return List.of();
        }
        return entries.subList(from, Math.min(from + PAGE_SIZE, entries.size()));
    }
}
