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
import io.avaje.config.Configuration;
import java.time.DateTimeException;
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
        Assertions.assertEquals(ZoneId.of("Europe/Berlin"), Config.getAs(DaytimeSettings.ZONE_KEY, DaytimeSettings::zone), "daytime.zone must default to Europe/Berlin");
    }

    @DisplayName("A valid zone id is parsed")
    @Test
    void validZoneIsParsed() {
        Assertions.assertEquals(ZoneId.of("Asia/Tokyo"), DaytimeSettings.zone("Asia/Tokyo"));
    }

    @DisplayName("An unknown zone id is rejected")
    @Test
    void unknownZoneIsRejected() {
        Assertions.assertThrows(DateTimeException.class, () -> DaytimeSettings.zone("Mars/Olympus"));
    }

    @DisplayName("Config.getAs names the key and keeps the offending zone in the cause")
    @Test
    void configGetAsNamesTheKeyForAnInvalidZone() {
        Configuration configuration = Configuration.builder().put(DaytimeSettings.ZONE_KEY, "Mars/Olympus").build();

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> configuration.getAs(DaytimeSettings.ZONE_KEY, DaytimeSettings::zone));

        Assertions.assertTrue(thrown.getMessage().contains(DaytimeSettings.ZONE_KEY), "the message must name " + DaytimeSettings.ZONE_KEY + ", was: " + thrown.getMessage());
        Assertions.assertTrue(thrown.getCause().getMessage().contains("Mars/Olympus"), "the cause must keep the offending value, was: " + thrown.getCause().getMessage());
    }
}
