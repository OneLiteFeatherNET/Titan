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
package net.onelitefeather.titan.core.settings;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SettingValueTest {

    @DisplayName("A flag and a level are different kinds of value, even with the same number")
    @Test
    void flagAndLevelAreDistinct() {
        Assertions.assertNotEquals(new SettingValue.Flag(true), new SettingValue.Level(1), "a flag must never equal a level");
    }

    @DisplayName("Two flags with the same state are equal")
    @Test
    void equalFlagsAreEqual() {
        Assertions.assertEquals(new SettingValue.Flag(true), new SettingValue.Flag(true), "flags with the same state must be equal");
    }

    @DisplayName("Two levels with the same whole number are equal")
    @Test
    void equalLevelsAreEqual() {
        Assertions.assertEquals(new SettingValue.Level(75), new SettingValue.Level(75), "levels with the same number must be equal");
    }
}
