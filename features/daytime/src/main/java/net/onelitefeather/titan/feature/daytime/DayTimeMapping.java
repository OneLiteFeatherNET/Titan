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

import java.time.Instant;
import java.time.ZoneId;

/**
 * Maps a wall-clock time linearly onto a Minecraft day: 06:00 is tick 0 (sunrise), 12:00 tick
 * 6000, 18:00 tick 12000 and 00:00 tick 18000.
 *
 * <p>Only the instant and the zone go in, so the result is the same after a restart, and it
 * follows the wall clock through daylight-saving changes.
 */
final class DayTimeMapping {

    static final long TICKS_PER_DAY = 24000L;
    static final long NOON_TICKS = 6000L;

    private static final long SECONDS_PER_DAY = 86400L;
    private static final long SUNRISE_SECOND_OF_DAY = 6L * 3600L;

    long ticksAt(Instant instant, ZoneId zone) {
        long secondOfDay = instant.atZone(zone).toLocalTime().toSecondOfDay();
        long secondsSinceSunrise = Math.floorMod(secondOfDay - SUNRISE_SECOND_OF_DAY, SECONDS_PER_DAY);
        return secondsSinceSunrise * TICKS_PER_DAY / SECONDS_PER_DAY;
    }
}
