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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SettingsLayoutTest {

    @DisplayName("A single option sits in the middle column")
    @Test
    void singleOptionIsCentred() {
        Assertions.assertEquals(4, SettingsLayout.column(0, 1), "one option must sit in column 4");
    }

    @DisplayName("Two options sit two columns apart around the middle")
    @Test
    void twoOptionsAreSpreadAroundTheMiddle() {
        Assertions.assertEquals(3, SettingsLayout.column(0, 2), "the first of two options must sit in column 3");
        Assertions.assertEquals(5, SettingsLayout.column(1, 2), "the second of two options must sit in column 5");
    }

    @DisplayName("Nine options fill every column once")
    @Test
    void nineOptionsFillEveryColumn() {
        for (int index = 0; index < 9; index++) {
            Assertions.assertEquals(index, SettingsLayout.column(index, 9), "option " + index + " of nine must sit in column " + index);
        }
    }

    @DisplayName("An option's icon is in the first row and its switch in the second, in the same column")
    @Test
    void iconAndSwitchShareAColumn() {
        Assertions.assertEquals(12, SettingsLayout.iconSlot(0, 2), "the icon of the first of two options must be slot 12");
        Assertions.assertEquals(21, SettingsLayout.toggleSlot(0, 2), "the switch of the first of two options must be slot 21");
    }

    @DisplayName("More than nine options are rejected because they do not fit one row")
    @Test
    void tenOptionsAreRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SettingsLayout.column(0, 10), "ten options must not fit the menu");
    }
}
