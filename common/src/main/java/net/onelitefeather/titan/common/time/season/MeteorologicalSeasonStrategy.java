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
import java.time.Month;

/**
 * Puts the season boundaries on the fixed month starts 1 March, 1 June, 1 September and 1 December.
 *
 * <p>This is the default of stage 2 (US-2.11). The boundaries fall on calendar days that never
 * move,
 * so nothing has to be computed and nothing can drift: a build team can be told "the winter world
 * goes live on the first of December" and that is the whole rule. The astronomical boundaries
 * differ
 * by roughly three weeks — noticeable, but not a reason to pull an astronomical calculation into
 * the
 * default path.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class MeteorologicalSeasonStrategy implements SeasonBoundaryStrategy {

    private static final MeteorologicalSeasonStrategy INSTANCE = new MeteorologicalSeasonStrategy();

    private MeteorologicalSeasonStrategy() {
    }

    /**
     * Returns the shared instance.
     *
     * <p>The strategy carries no state, so a single instance serves every caller.
     *
     * @return the shared meteorological strategy
     */
    @Contract(pure = true)
    public static MeteorologicalSeasonStrategy instance() {
        return INSTANCE;
    }

    @Override
    @Contract(pure = true)
    public Season seasonAt(LocalDate date) {
        return switch (Month.of(date.getMonthValue())) {
            case MARCH, APRIL, MAY -> Season.SPRING;
            case JUNE, JULY, AUGUST -> Season.SUMMER;
            case SEPTEMBER, OCTOBER, NOVEMBER -> Season.AUTUMN;
            case DECEMBER, JANUARY, FEBRUARY -> Season.WINTER;
        };
    }

    @Override
    public String toString() {
        return "MeteorologicalSeasonStrategy";
    }
}
