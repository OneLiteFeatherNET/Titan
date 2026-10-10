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
package net.onelitefeather.titan.feature.lobbyswitcher;

import io.avaje.config.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LobbySwitcherSettingsTest {

    @DisplayName("The shipped default is 5 seconds")
    @Test
    void shippedDefaultIsFive() {
        Configuration configuration = Configuration.builder().load("application-test.yaml").build();

        int seconds = configuration.getAs(LobbySwitcherSettings.REFRESH_KEY, LobbySwitcherSettings::refreshSeconds);

        Assertions.assertEquals(5, seconds, "the column's own lobbyswitcher.yaml default");
    }

    @DisplayName("1 and 3600 are valid")
    @Test
    void bordersAreValid() {
        Assertions.assertEquals(1, LobbySwitcherSettings.refreshSeconds("1"), "lower border");
        Assertions.assertEquals(3600, LobbySwitcherSettings.refreshSeconds("3600"), "upper border");
    }

    @DisplayName("0 is rejected, naming the key and the value")
    @Test
    void zeroIsRejected() {
        assertRejected("0");
    }

    @DisplayName("3601 is rejected, naming the key and the value")
    @Test
    void aboveTheCapIsRejected() {
        assertRejected("3601");
    }

    @DisplayName("'fünf' is rejected, naming the key and the value")
    @Test
    void nonNumericIsRejected() {
        assertRejected("fünf");
    }

    @DisplayName("Config.getAs keeps the key in the message for an invalid configured value")
    @Test
    void configNamesTheKey() {
        Configuration configuration = Configuration.builder().put(LobbySwitcherSettings.REFRESH_KEY, "0").build();

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> configuration.getAs(LobbySwitcherSettings.REFRESH_KEY, LobbySwitcherSettings::refreshSeconds), "an invalid value aborts startup");

        Assertions.assertTrue(thrown.getMessage().contains(LobbySwitcherSettings.REFRESH_KEY), "the message must name the key, was: " + thrown.getMessage());
    }

    private static void assertRejected(String raw) {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> LobbySwitcherSettings.refreshSeconds(raw), raw + " must be rejected");

        Assertions.assertTrue(thrown.getMessage().contains(LobbySwitcherSettings.REFRESH_KEY) && thrown.getMessage().contains(raw), "the message must name the key and " + raw + ", was: " + thrown.getMessage());
    }
}
