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

class LevelSettingsRegistryTest {

    private static final LevelSetting VOLUME = new LevelSetting(SettingKey.parse("titan:radio.volume"), List.of(25, 50, 75, 100), 100);
    private static final BooleanSetting TAG = new BooleanSetting(SettingKey.parse("titan:tag.enabled"), SettingDefault.OPT_IN);

    @DisplayName("A registered level setting is found by its key")
    @Test
    void registeredLevelIsFound() {
        SettingsRegistry registry = new SettingsRegistry();

        registry.registerLevel("radio", VOLUME);

        Assertions.assertEquals(VOLUME, registry.findLevel(VOLUME.key()).orElseThrow(), "the level must be found under its key");
    }

    @DisplayName("Level settings are not listed in the settings menu")
    @Test
    void levelsAreNotInTheDefinitions() {
        SettingsRegistry registry = new SettingsRegistry();
        registry.registerLevel("radio", VOLUME);

        Assertions.assertTrue(registry.definitions().isEmpty(), "a level must not show up as a switch in the settings menu");
    }

    @DisplayName("A key taken by a switch cannot be registered again as a level")
    @Test
    void keyCollisionAcrossKindsIsRejected() {
        SettingsRegistry registry = new SettingsRegistry();
        registry.register("tag", TAG);
        LevelSetting clash = new LevelSetting(TAG.key(), List.of(25, 100), 100);

        Assertions.assertThrows(IllegalStateException.class, () -> registry.registerLevel("radio", clash), "a level must not reuse the key of a switch");
    }

    @DisplayName("A key taken by a level cannot be registered again as a switch")
    @Test
    void keyCollisionOtherWayRoundIsRejected() {
        SettingsRegistry registry = new SettingsRegistry();
        registry.registerLevel("radio", VOLUME);
        BooleanSetting clash = new BooleanSetting(VOLUME.key(), SettingDefault.OPT_IN);

        Assertions.assertThrows(IllegalStateException.class, () -> registry.register("tag", clash), "a switch must not reuse the key of a level");
    }

    @DisplayName("Levels do not count against the menu limit of switches")
    @Test
    void levelsDoNotUseTheSwitchLimit() {
        SettingsRegistry registry = new SettingsRegistry();
        for (int index = 0; index < SettingsRegistry.MAX_SETTINGS; index++) {
            registry.register("column-" + index, new BooleanSetting(SettingKey.parse("titan:option." + index), SettingDefault.OPT_IN));
        }

        Assertions.assertDoesNotThrow(() -> registry.registerLevel("radio", VOLUME), "a level must not be refused because the switch row is full");
    }
}
