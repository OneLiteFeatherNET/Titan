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
package net.onelitefeather.titan.common.time;

import net.onelitefeather.titan.common.time.season.Season;
import net.onelitefeather.titan.common.time.season.SeasonBoundaryStrategy;
import org.jetbrains.annotations.Contract;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Provides the season the lobby is currently in as read-only state (US-2.09).
 *
 * <p>Like {@link WorldTimeService} it holds the {@link Clock} that the strategies deliberately do
 * not, so that "which season is it" can be asked for any instant a test cares to name (US-2.03).
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public interface SeasonService {

    /**
     * Creates a service on the meteorological boundaries in the editorial zone — the stage 2
     * default.
     *
     * @param clock the clock the current date is read from
     * @return a new service
     */
    @Contract(pure = true, value = "_ -> new")
    static SeasonService create(Clock clock) {
        return create(clock, TitanTime.EDITORIAL_ZONE, SeasonBoundaryStrategy.meteorological());
    }

    /**
     * Creates a service on the given boundaries in the editorial zone.
     *
     * @param clock    the clock the current date is read from
     * @param strategy the boundaries to apply
     * @return a new service
     */
    @Contract(pure = true, value = "_, _ -> new")
    static SeasonService create(Clock clock, SeasonBoundaryStrategy strategy) {
        return create(clock, TitanTime.EDITORIAL_ZONE, strategy);
    }

    /**
     * Creates a service on the given boundaries in the given zone.
     *
     * @param clock    the clock the current date is read from
     * @param zone     the zone the date is read in
     * @param strategy the boundaries to apply
     * @return a new service
     */
    @Contract(pure = true, value = "_, _, _ -> new")
    static SeasonService create(Clock clock, ZoneId zone, SeasonBoundaryStrategy strategy) {
        return new TitanSeasonService(clock, zone, strategy);
    }

    /**
     * Returns the season in effect at the clock's current instant.
     *
     * @return the current season
     */
    Season currentSeason();

    /**
     * Returns the date the clock's current instant falls on in this service's zone.
     *
     * @return the current date
     */
    LocalDate currentDate();

    /**
     * Returns the zone the date is read in.
     *
     * @return the zone
     */
    @Contract(pure = true)
    ZoneId zone();

    /**
     * Returns the boundaries in use.
     *
     * @return the strategy
     */
    @Contract(pure = true)
    SeasonBoundaryStrategy strategy();
}
