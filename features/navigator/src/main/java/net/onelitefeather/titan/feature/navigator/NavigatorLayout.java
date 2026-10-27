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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.Material;

/**
 * The navigator's grid: four rows of nine slots with a dark glass frame on the outer ring. Slots
 * count from 0 in the top left corner, row by row.
 */
final class NavigatorLayout {

    static final InventoryType TYPE = InventoryType.CHEST_4_ROW;
    static final int COLUMNS = 9;
    static final int ROWS = 4;
    static final int SIZE = COLUMNS * ROWS;
    /**
     * The middle of the bottom row, which is part of the frame and takes the spawn entry instead.
     */
    static final int SPAWN_SLOT = 31;
    /**
     * The staging hub on the main grid: it opens the staging submenu and shows only with a staging
     * permission. It takes the bottom right corner of the frame.
     */
    static final int STAGING_HUB_SLOT = 35;
    /** Every frame pane is the same dark glass, so the frame reads as one piece. */
    static final Material FRAME_MATERIAL = Material.BLACK_STAINED_GLASS_PANE;
    // The minigames centre in row one and the servers in row two, each group seven slots wide.
    private static final int MINIGAME_START = 10;
    private static final int SERVER_START = 19;
    private static final int GROUP_WIDTH = 7;

    private NavigatorLayout() {
    }

    /**
     * Whether a slot belongs to the outer ring: the top and bottom row and the first and last
     * column.
     */
    static boolean isFrame(int slot) {
        int row = slot / COLUMNS;
        int column = slot % COLUMNS;
        return row == 0 || row == ROWS - 1 || column == 0 || column == COLUMNS - 1;
    }

    /**
     * The slot of every visible main-grid entry: each group centred in its row, in the order the
     * entries are given, without a gap for a hidden entry.
     */
    static Map<Destination, Integer> mainSlots(List<Destination> visible) {
        Map<Destination, Integer> slots = new EnumMap<>(Destination.class);
        place(slots, visible, Destination.Group.MINIGAME, MINIGAME_START);
        place(slots, visible, Destination.Group.SERVER, SERVER_START);
        return slots;
    }

    /**
     * The slots of {@code count} entries in a group that starts at {@code start}, centred in its
     * width.
     */
    static List<Integer> centred(int start, int count) {
        int first = start + (GROUP_WIDTH - count) / 2;
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            slots.add(first + i);
        }
        return List.copyOf(slots);
    }

    private static void place(Map<Destination, Integer> slots, List<Destination> visible, Destination.Group group, int start) {
        List<Destination> members = visible.stream().filter(destination -> destination.group() == group).toList();
        List<Integer> at = centred(start, members.size());
        for (int i = 0; i < members.size(); i++) {
            slots.put(members.get(i), at.get(i));
        }
    }
}
