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
package net.onelitefeather.titan.feature.settings;

import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SettingsOptionsTest {

    @DisplayName("The shipped defaults keep the feature off and the settings in memory")
    @Test
    void shippedDefaultsAreOffAndInMemory() {
        SettingsOptions options = SettingsOptions.read();
        Assertions.assertFalse(options.enabled(), "settings.enabled must default to false");
        Assertions.assertFalse(options.persistent(), "an empty otis.url must keep the settings in memory");
        Assertions.assertEquals(Duration.ofMillis(2000), options.writeDebounce(), "the shipped write debounce is 2000 ms");
        Assertions.assertEquals(3, options.writeRetries(), "the shipped write retries are 3");
    }

    @DisplayName("A blank Otis url selects the in-memory store")
    @Test
    void blankUrlIsNotPersistent() {
        Assertions.assertFalse(SettingsOptions.of(true, "  ", 2000, 3).persistent(), "a blank url must not select Otis");
    }

    @DisplayName("A set Otis url selects the Otis store")
    @Test
    void setUrlIsPersistent() {
        Assertions.assertTrue(SettingsOptions.of(true, "http://otis:8080", 2000, 3).persistent(), "a url must select Otis");
    }

    @DisplayName("A negative write debounce aborts the start")
    @Test
    void negativeDebounceIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SettingsOptions.of(true, "", -1, 3), "a negative debounce is not a valid setting");
    }
}
