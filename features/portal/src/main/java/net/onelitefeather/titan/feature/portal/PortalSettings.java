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
 * @param labelRefreshSeconds whole seconds between two reads, at least 1
 */
record PortalSettings(int labelRefreshSeconds) {

    static final String REFRESH_KEY = "portal.labelRefreshSeconds";

    /**
     * Aborts startup, naming the key, when the configured value is not a whole number of at least
     * 1.
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
        return seconds;
    }
}
