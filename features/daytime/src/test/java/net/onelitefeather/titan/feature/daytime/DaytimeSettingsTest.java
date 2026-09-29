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
package net.onelitefeather.titan.feature.daytime;

import io.avaje.config.Config;
import java.time.ZoneId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DaytimeSettingsTest {

    @DisplayName("The shipped defaults enable the module")
    @Test
    void shippedDefaultEnablesTheModule() {
        Assertions.assertTrue(Config.getBool(DaytimeSettings.ENABLED_KEY), "daytime.enabled must default to true");
    }

    @DisplayName("The shipped defaults use Europe/Berlin")
    @Test
    void shippedDefaultZoneIsBerlin() {
        Assertions.assertEquals(ZoneId.of("Europe/Berlin"), Config.getAs(DaytimeSettings.ZONE_KEY, ZoneId::of), "daytime.zone must default to Europe/Berlin");
    }
}
