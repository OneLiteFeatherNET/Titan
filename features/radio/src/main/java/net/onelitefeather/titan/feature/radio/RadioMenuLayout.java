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

import net.minestom.server.inventory.InventoryType;

/**
 * The slots of a radio menu: three rows, the discs from the top left, the four controls in the
 * bottom row. Both menus share the control slots, so one click table serves both.
 */
final class RadioMenuLayout {

    static final InventoryType TYPE = InventoryType.CHEST_3_ROW;
    static final int SIZE = 27;
    static final int STOP_SLOT = 22;
    static final int NEXT_SLOT = 23;
    static final int MODE_SLOT = 24;
    static final int SWITCH_SLOT = 26;

    /** What a click on a slot means. */
    enum Control {
        STOP, NEXT, MODE, SWITCH, NONE
    }

    private RadioMenuLayout() {
    }

    /**
     * The number of disc slots of {@code menu}: the vanilla discs fill 22, the songs at most 18.
     */
    static int discSlots(RadioMenu menu) {
        return menu == RadioMenu.VANILLA ? VanillaDiscs.ALL.size() : RadioLimits.MAX_SONGS;
    }

    /** The index of the disc a click on {@code slot} means, or -1 when the slot is no disc slot. */
    static int discAt(RadioMenu menu, int slot) {
        return slot >= 0 && slot < discSlots(menu) ? slot : -1;
    }

    static Control controlAt(int slot) {
        return switch (slot) {
            case STOP_SLOT -> Control.STOP;
            case NEXT_SLOT -> Control.NEXT;
            case MODE_SLOT -> Control.MODE;
            case SWITCH_SLOT -> Control.SWITCH;
            default -> Control.NONE;
        };
    }
}
