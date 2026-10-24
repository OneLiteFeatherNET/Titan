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

import io.avaje.config.Config;
import java.time.Duration;

/**
 * The {@code settings} section: whether the feature is on, whether settings are kept in Otis or
 * only in memory, and how long a switch waits before it is saved.
 */
record SettingsOptions(boolean enabled, boolean persistent, Duration writeDebounce,
                       int writeRetries) {

    static final String ENABLED_KEY = "settings.enabled";
    static final String OTIS_URL_KEY = "otis.url";
    static final String WRITE_DEBOUNCE_KEY = "otis.writeDebounceMillis";
    static final String WRITE_RETRIES_KEY = "otis.writeRetries";

    SettingsOptions {
        if (writeDebounce.isNegative()) {
            throw new IllegalArgumentException(WRITE_DEBOUNCE_KEY + " must not be negative, was " + writeDebounce.toMillis());
        }
        if (writeRetries < 0) {
            throw new IllegalArgumentException(WRITE_RETRIES_KEY + " must not be negative, was " + writeRetries);
        }
    }

    /**
     * @param otisUrl the Otis base url; blank means the settings are kept in memory only
     */
    static SettingsOptions of(boolean enabled, String otisUrl, long writeDebounceMillis, int writeRetries) {
        boolean persistent = otisUrl != null && !otisUrl.isBlank();
        return new SettingsOptions(enabled, persistent, Duration.ofMillis(writeDebounceMillis), writeRetries);
    }

    /** Reads the section from the configuration; an invalid value aborts the start. */
    static SettingsOptions read() {
        boolean enabled = Config.getBool(ENABLED_KEY, false);
        String otisUrl = Config.get(OTIS_URL_KEY, "");
        long writeDebounceMillis = Config.getAs(WRITE_DEBOUNCE_KEY, Long::parseLong);
        int writeRetries = Config.getAs(WRITE_RETRIES_KEY, Integer::parseInt);
        return of(enabled, otisUrl, writeDebounceMillis, writeRetries);
    }
}
