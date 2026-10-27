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

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LevelSettingTest {

    private static final SettingKey KEY = SettingKey.parse("titan:radio.volume");

    @DisplayName("The default level is one of the levels")
    @Test
    void defaultIsInTheList() {
        LevelSetting setting = new LevelSetting(KEY, List.of(25, 50, 75, 100), 100);

        Assertions.assertTrue(setting.levels().contains(setting.defaultLevel()), "the default level must be one of the levels");
    }

    @DisplayName("A level list that is not sorted is sorted")
    @Test
    void unsortedLevelsAreSorted() {
        LevelSetting setting = new LevelSetting(KEY, List.of(75, 25, 100, 50), 100);

        Assertions.assertEquals(List.of(25, 50, 75, 100), setting.levels(), "the levels must be kept in ascending order");
    }

    @DisplayName("An empty level list is rejected")
    @Test
    void emptyLevelsAreRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LevelSetting(KEY, List.of(), 0), "a level setting needs at least one level");
    }

    @DisplayName("A level list with a duplicate is rejected")
    @Test
    void duplicateLevelsAreRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LevelSetting(KEY, List.of(25, 25, 100), 100), "a level must not be listed twice");
    }

    @DisplayName("A default outside the list is rejected")
    @Test
    void defaultOutsideTheListIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LevelSetting(KEY, List.of(25, 50, 75, 100), 60), "a default that is not a level must be rejected");
    }
}
