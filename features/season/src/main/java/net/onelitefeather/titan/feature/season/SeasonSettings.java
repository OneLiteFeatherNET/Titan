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
package net.onelitefeather.titan.feature.season;

/**
 * The {@code season} module's configuration keys.
 */
final class SeasonSettings {

    static final String PREFIX = "seasons.";
    static final String ZONE_KEY = "seasons.zone";
    /** Reserved: {@code seasons.zone} would collide with a season of that id. */
    static final String RESERVED_ID = "zone";

    static final String WORLD_FIELD = "world";
    static final String FROM_FIELD = "from";
    static final String TO_FIELD = "to";
    static final String ENABLED_FIELD = "enabled";

    static final String WORLDS_DIRECTORY = "worlds";
    // Kept here because a column sees only core; an apps test pins it to MapEntry.MAP_FILE_NAME.
    static final String MAP_FILE_NAME = "map.json";

    private SeasonSettings() {
    }

    static String key(String id, String field) {
        return PREFIX + id + "." + field;
    }
}
