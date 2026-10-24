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
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SettingsRegistryTest {

    private static final BooleanSetting TAG = new BooleanSetting(SettingKey.parse("titan:tag.enabled"), SettingDefault.OPT_IN);
    private static final BooleanSetting THROW = new BooleanSetting(SettingKey.parse("titan:throw.target"), SettingDefault.OPT_OUT);

    @DisplayName("Definitions come back in the order the columns registered them")
    @Test
    void definitionsKeepRegistrationOrder() {
        SettingsRegistry registry = new SettingsRegistry();
        registry.register("lobby-tag", TAG);
        registry.register("lobby-throw", THROW);
        Assertions.assertEquals(List.of(TAG, THROW), registry.definitions(), "the menu shows the options in registration order");
    }

    @DisplayName("A key registered twice is refused, naming both registrations")
    @Test
    void duplicateKeyNamesBothRegistrations() {
        SettingsRegistry registry = new SettingsRegistry();
        registry.register("lobby-tag", TAG);
        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () -> registry.register("other-column", TAG), "a second registration of the same key must fail");
        Assertions.assertTrue(failure.getMessage().contains("'lobby-tag'"), "the message must name the first registration: " + failure.getMessage());
        Assertions.assertTrue(failure.getMessage().contains("'other-column'"), "the message must name the second registration: " + failure.getMessage());
    }

    @DisplayName("An unknown key finds nothing")
    @Test
    void unknownKeyIsEmpty() {
        SettingsRegistry registry = new SettingsRegistry();
        registry.register("lobby-tag", TAG);
        Optional<BooleanSetting> found = registry.find(SettingKey.parse("titan:unknown.key"));
        Assertions.assertTrue(found.isEmpty(), "an unregistered key must not resolve to a setting");
    }

    @DisplayName("A registered key is found with its definition")
    @Test
    void knownKeyIsFound() {
        SettingsRegistry registry = new SettingsRegistry();
        registry.register("lobby-tag", TAG);
        Assertions.assertEquals(Optional.of(TAG), registry.find(TAG.key()), "the registered definition must be returned");
    }

    @DisplayName("A registry refuses an option beyond the menu's row")
    @Test
    void fullRegistryIsRefused() {
        SettingsRegistry registry = new SettingsRegistry();
        for (int index = 0; index < SettingsRegistry.MAX_SETTINGS; index++) {
            registry.register("column-" + index, new BooleanSetting(SettingKey.parse("titan:option." + index), SettingDefault.OPT_IN));
        }
        Assertions.assertThrows(IllegalStateException.class, () -> registry.register("overflow", TAG), "the menu has one row, so a tenth option must be refused");
    }
}
