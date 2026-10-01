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
package net.onelitefeather.titan.feature.portal;

import io.avaje.config.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PortalSettingsTest {

    @DisplayName("The shipped default is 5 seconds")
    @Test
    void shippedDefaultIsFive() {
        Configuration configuration = Configuration.builder().load("application-test.yaml").build();

        Assertions.assertEquals(5, configuration.getAs(PortalSettings.REFRESH_KEY, PortalSettings::refreshSeconds), "the column's own portal.yaml default");
    }

    @DisplayName("2 and 1 are valid")
    @Test
    void positiveWholeNumbersAreValid() {
        Assertions.assertEquals(2, PortalSettings.refreshSeconds("2"));
        Assertions.assertEquals(1, PortalSettings.refreshSeconds("1"));
    }

    @DisplayName("0 and -1 are rejected, naming the key and the value")
    @Test
    void belowOneIsRejected() {
        for (String raw : new String[]{"0", "-1"}) {
            IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> PortalSettings.refreshSeconds(raw));

            Assertions.assertTrue(thrown.getMessage().contains(PortalSettings.REFRESH_KEY) && thrown.getMessage().contains(raw), "the message must name the key and " + raw + ", was: " + thrown.getMessage());
        }
    }

    @DisplayName("'abc' is rejected, naming the key and the value")
    @Test
    void nonNumericIsRejected() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> PortalSettings.refreshSeconds("abc"));

        Assertions.assertTrue(thrown.getMessage().contains(PortalSettings.REFRESH_KEY) && thrown.getMessage().contains("abc"), "the message must name the key and the value, was: " + thrown.getMessage());
    }

    @DisplayName("Config.getAs keeps the key in the message for an invalid configured value")
    @Test
    void configNamesTheKey() {
        Configuration configuration = Configuration.builder().put(PortalSettings.REFRESH_KEY, "0").build();

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> configuration.getAs(PortalSettings.REFRESH_KEY, PortalSettings::refreshSeconds));

        Assertions.assertTrue(thrown.getMessage().contains(PortalSettings.REFRESH_KEY), "the message must name the key, was: " + thrown.getMessage());
    }
}
