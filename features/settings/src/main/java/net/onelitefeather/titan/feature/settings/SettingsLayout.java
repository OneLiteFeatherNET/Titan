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
package net.onelitefeather.titan.feature.settings;

/**
 * Where the options sit in the settings menu. The menu has three rows: the frame, the feature icons
 * and the switches. An option takes one column in the icon row and the switch row below it.
 *
 * <p>Up to five options are two columns apart, so they spread over the row; six to nine options
 * take adjacent columns. Both are centred on the middle column.
 */
final class SettingsLayout {

    static final int COLUMNS = 9;
    static final int MAX_OPTIONS = COLUMNS;
    static final int ICON_ROW = 1;
    static final int SWITCH_ROW = 2;

    private static final int MIDDLE_COLUMN = COLUMNS / 2;
    private static final int SPREAD_UP_TO = 5;

    private SettingsLayout() {
    }

    /**
     * @param index the position of the option in the menu, from 0
     * @param count how many options the menu lists
     * @return the column of the option, from 0 to {@link #COLUMNS} - 1
     */
    static int column(int index, int count) {
        if (count > MAX_OPTIONS) {
            throw new IllegalArgumentException("the settings menu fits " + MAX_OPTIONS + " options, but lists " + count);
        }
        int spacing = count <= SPREAD_UP_TO ? 2 : 1;
        return MIDDLE_COLUMN - (count - 1) * spacing / 2 + index * spacing;
    }

    /** The slot of the option's feature icon, in the icon row. */
    static int iconSlot(int index, int count) {
        return ICON_ROW * COLUMNS + column(index, count);
    }

    /** The slot of the option's switch, directly below its icon. */
    static int toggleSlot(int index, int count) {
        return SWITCH_ROW * COLUMNS + column(index, count);
    }
}
