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

import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** How many inventory rows a lobby list needs, and which entries still fit. */
final class SwitcherLayout {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwitcherLayout.class);

    static final int SLOTS_PER_ROW = 9;
    static final int MAX_ROWS = 6;
    static final int MAX_ENTRIES = SLOTS_PER_ROW * MAX_ROWS;

    private SwitcherLayout() {
    }

    /** One row for up to nine entries, one more per further nine, at most six. */
    static int rows(int entries) {
        int needed = (entries + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW;
        return Math.clamp(needed, 1, MAX_ROWS);
    }

    /** The first {@value #MAX_ENTRIES} entries by name; reports it when some had to be left out. */
    static List<SwitcherEntry> fit(List<SwitcherEntry> entries) {
        List<SwitcherEntry> byName = entries.stream().sorted(Comparator.comparing(SwitcherEntry::name)).toList();
        if (byName.size() <= MAX_ENTRIES) {
            return byName;
        }
        LOGGER.warn("Lobby switcher lists {} lobbies but fits {}; the others are left out", byName.size(), MAX_ENTRIES);
        return byName.subList(0, MAX_ENTRIES);
    }
}
