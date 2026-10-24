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

class BooleanSettingTest {

    private static final SettingKey KEY = SettingKey.parse("titan:tag.enabled");

    @DisplayName("An opt-in setting defaults to off")
    @Test
    void optInDefaultsToOff() {
        Assertions.assertFalse(new BooleanSetting(KEY, SettingDefault.OPT_IN).defaultValue(), "opt-in must be off until the player switches it on");
    }

    @DisplayName("An opt-out setting defaults to on")
    @Test
    void optOutDefaultsToOn() {
        Assertions.assertTrue(new BooleanSetting(KEY, SettingDefault.OPT_OUT).defaultValue(), "opt-out must be on until the player switches it off");
    }

    @DisplayName("Two settings with the same key, default and mode are equal")
    @Test
    void equalityFollowsKeyAndDefault() {
        Assertions.assertEquals(new BooleanSetting(KEY, SettingDefault.OPT_IN), new BooleanSetting(SettingKey.parse("titan:tag.enabled"), SettingDefault.OPT_IN), "a setting is its key and default");
    }

    @DisplayName("A setting without a key is rejected")
    @Test
    void missingKeyIsRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> new BooleanSetting(null, SettingDefault.OPT_IN), "the key is required");
    }
}
