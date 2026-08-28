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

/**
 * Always answers with the same season, whatever the date (US-2.13).
 *
 * <p>This is not only a test aid. It is the supported way to show a winter event in August without
 * anyone reaching for the system clock: pin the season, look at the result, unpin it. That the
 * preview path costs ten lines instead of a third branch in a conditional is the concrete payoff of
 * building the boundaries as a strategy.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public record FixedSeasonStrategy(Season season) implements SeasonBoundaryStrategy {

    /**
     * Returns a strategy pinned to the given season.
     *
     * @param season the season to answer with
     * @return the pinned strategy
     */
    @Contract(pure = true, value = "_ -> new")
    public static FixedSeasonStrategy of(Season season) {
        return new FixedSeasonStrategy(season);
    }

    @Override
    @Contract(pure = true)
    public Season seasonAt(LocalDate date) {
        return this.season;
    }
}
