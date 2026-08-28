/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.time.season;

import org.jetbrains.annotations.Contract;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Decides which season applies on a date.
 *
 * <p>Implementations are stateless and pure: they are handed the date instead of reading a clock
 * themselves. The {@link java.time.Clock} lives in the calling service (US-2.03), which is what
 * lets
 * a test check December behaviour in August without changing the machine.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public interface SeasonBoundaryStrategy {

    /**
     * Returns the season in effect on the given date.
     *
     * @param date the date in the editorial time zone
     * @return the season that applies that day
     */
    @Contract(pure = true)
    Season seasonAt(LocalDate date);

    /**
     * Returns the meteorological boundaries, the default of stage 2 (US-2.11).
     *
     * @return the shared, immutable meteorological strategy
     */
    @Contract(pure = true)
    static SeasonBoundaryStrategy meteorological() {
        return MeteorologicalSeasonStrategy.instance();
    }

    /**
     * Returns the astronomical boundaries for the given zone (US-2.12).
     *
     * <p>An equinox is an instant, not a date, so the zone decides which calendar day it lands on.
     *
     * @param zone the zone the boundary instants are resolved in
     * @return an astronomical strategy for that zone
     */
    @Contract(pure = true)
    static SeasonBoundaryStrategy astronomical(ZoneId zone) {
        return AstronomicalSeasonStrategy.of(zone);
    }

    /**
     * Returns a strategy that always answers with the same season (US-2.13).
     *
     * @param season the season to pin
     * @return a strategy that ignores the date
     */
    @Contract(pure = true)
    static SeasonBoundaryStrategy fixed(Season season) {
        return FixedSeasonStrategy.of(season);
    }
}
