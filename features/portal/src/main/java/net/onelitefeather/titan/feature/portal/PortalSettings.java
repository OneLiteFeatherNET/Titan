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

import io.avaje.config.Config;

/**
 * The portal module's configuration: how often the label counts are read.
 *
 * @param labelRefreshSeconds whole seconds between two reads, from 1 to
 *                            {@value #MAX_REFRESH_SECONDS}
 */
record PortalSettings(int labelRefreshSeconds) {

    static final String REFRESH_KEY = "portal.labelRefreshSeconds";
    // An hour is far beyond any useful label age and keeps seconds * ticks well inside an int.
    static final int MAX_REFRESH_SECONDS = 3600;

    /**
     * Aborts startup, naming the key, when the configured value is not a whole number from 1 to
     * {@value #MAX_REFRESH_SECONDS}.
     */
    static PortalSettings read() {
        return new PortalSettings(Config.getAs(REFRESH_KEY, PortalSettings::refreshSeconds));
    }

    static int refreshSeconds(String raw) {
        int seconds;
        try {
            seconds = Integer.parseInt(raw.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(REFRESH_KEY + " must be a whole number, was '" + raw + "'", e);
        }
        if (seconds < 1) {
            throw new IllegalArgumentException(REFRESH_KEY + " must be at least 1, was " + seconds);
        }
        if (seconds > MAX_REFRESH_SECONDS) {
            throw new IllegalArgumentException(REFRESH_KEY + " must be at most " + MAX_REFRESH_SECONDS + ", was " + seconds);
        }
        return seconds;
    }
}
