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

import java.time.ZoneId;

/**
 * The {@code daytime} module's configuration keys and the pure parsing of its zone value.
 */
final class DaytimeSettings {

    static final String ENABLED_KEY = "daytime.enabled";
    static final String ZONE_KEY = "daytime.zone";

    private DaytimeSettings() {
    }

    /**
     * @throws java.time.DateTimeException if {@code raw} is not a known zone id
     */
    static ZoneId zone(String raw) {
        return ZoneId.of(raw);
    }
}
