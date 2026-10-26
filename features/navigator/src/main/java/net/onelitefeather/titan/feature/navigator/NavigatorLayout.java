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

import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.Material;

/**
 * The navigator's grid: four rows of nine slots with a glass frame on the outer ring. Slots count
 * from 0 in the top left corner, row by row.
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
     * permission.
     */
    static final int STAGING_HUB_SLOT = 16;

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

    /** The glass pane of a frame slot: light blue and cyan alternate by row plus column. */
    static Material frameMaterial(int slot) {
        int parity = (slot / COLUMNS + slot % COLUMNS) % 2;
        return parity == 0 ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.CYAN_STAINED_GLASS_PANE;
    }
}
