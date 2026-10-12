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
package net.onelitefeather.titan.feature.navigator;

import io.avaje.config.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NavigatorSettingsTest {

    @DisplayName("The shipped default is 5 seconds")
    @Test
    void shippedDefaultIsFive() {
        Configuration configuration = Configuration.builder().resourceLoader(getClass().getClassLoader()::getResourceAsStream).load("titan/defaults/navigator.yaml").build();

        int seconds = configuration.getAs(NavigatorSettings.REFRESH_KEY, NavigatorSettings::refreshSeconds);

        Assertions.assertEquals(5, seconds);
    }

    @DisplayName("1 and 3600 are valid")
    @Test
    void bordersAreValid() {
        Assertions.assertEquals(1, NavigatorSettings.refreshSeconds("1"));
        Assertions.assertEquals(3600, NavigatorSettings.refreshSeconds("3600"));
    }

    @DisplayName("0, 3601 and text are rejected, naming the key and the value")
    @Test
    void invalidValuesAreRejected() {
        for (String raw : new String[]{"0", "3601", "fünf"}) {
            IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> NavigatorSettings.refreshSeconds(raw), raw + " must be rejected");

            Assertions.assertTrue(thrown.getMessage().contains(NavigatorSettings.REFRESH_KEY) && thrown.getMessage().contains(raw), "the message must name the key and " + raw + ", was: " + thrown.getMessage());
        }
    }
}
