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

import io.avaje.config.Config;

/**
 * The navigator's configuration: how often the shown player counts are read, how long an empty
 * staging service runs before it is stopped again, and how long players wait for a staging service
 * to start.
 *
 * @param refreshSeconds             whole seconds between two reads
 * @param stagingIdleSeconds         whole seconds an empty staging service runs before its stop
 * @param stagingStartTimeoutSeconds whole seconds the players wait for a staging service before
 *                                   they are refused
 */
record NavigatorSettings(int refreshSeconds, int stagingIdleSeconds,
                         int stagingStartTimeoutSeconds) {

    static final String REFRESH_KEY = "navigator.refreshSeconds";
    static final String STAGING_IDLE_KEY = "navigator.stagingIdleSeconds";
    static final String STAGING_START_TIMEOUT_KEY = "navigator.stagingStartTimeoutSeconds";
    // An hour is far beyond any useful count age and keeps seconds * ticks well inside an int.
    static final int MAX_REFRESH_SECONDS = 3600;
    static final int MIN_STAGING_IDLE_SECONDS = 60;
    static final int MAX_STAGING_IDLE_SECONDS = 86400;
    static final int MIN_STAGING_START_TIMEOUT_SECONDS = 30;
    static final int MAX_STAGING_START_TIMEOUT_SECONDS = 900;

    /** Aborts startup, naming the key, when a configured value is not a whole number in range. */
    static NavigatorSettings read() {
        return new NavigatorSettings(Config.getAs(REFRESH_KEY, NavigatorSettings::refreshSeconds), Config.getAs(STAGING_IDLE_KEY, NavigatorSettings::stagingIdleSeconds), Config.getAs(STAGING_START_TIMEOUT_KEY, NavigatorSettings::stagingStartTimeoutSeconds));
    }

    static int refreshSeconds(String raw) {
        return wholeSeconds(REFRESH_KEY, raw, 1, MAX_REFRESH_SECONDS);
    }

    static int stagingIdleSeconds(String raw) {
        return wholeSeconds(STAGING_IDLE_KEY, raw, MIN_STAGING_IDLE_SECONDS, MAX_STAGING_IDLE_SECONDS);
    }

    static int stagingStartTimeoutSeconds(String raw) {
        return wholeSeconds(STAGING_START_TIMEOUT_KEY, raw, MIN_STAGING_START_TIMEOUT_SECONDS, MAX_STAGING_START_TIMEOUT_SECONDS);
    }

    private static int wholeSeconds(String key, String raw, int min, int max) {
        int seconds;
        try {
            seconds = Integer.parseInt(raw.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a whole number, was '" + raw + "'", e);
        }
        if (seconds < min) {
            throw new IllegalArgumentException(key + " must be at least " + min + ", was " + seconds);
        }
        if (seconds > max) {
            throw new IllegalArgumentException(key + " must be at most " + max + ", was " + seconds);
        }
        return seconds;
    }
}
