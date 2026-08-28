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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The meteorological boundaries, checked on the day before and the day of every switch.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
class MeteorologicalSeasonStrategyTest {

    private final SeasonBoundaryStrategy strategy = SeasonBoundaryStrategy.meteorological();

    @ParameterizedTest(name = "{0} is {1}")
    @CsvSource({"2026-01-01, WINTER", "2026-02-28, WINTER", "2026-03-01, SPRING", "2026-05-31, SPRING", "2026-06-01, SUMMER", "2026-08-31, SUMMER", "2026-09-01, AUTUMN", "2026-11-30, AUTUMN", "2026-12-01, WINTER", "2026-12-31, WINTER",
    })
    @DisplayName("The seasons change on the first of March, June, September and December")
    void theSeasonsChangeOnFixedMonthStarts(LocalDate date, Season expected) {
        assertEquals(expected, this.strategy.seasonAt(date));
    }

    @Test
    @DisplayName("A leap day is still winter")
    void aLeapDayIsStillWinter() {
        assertEquals(Season.WINTER, this.strategy.seasonAt(LocalDate.of(2028, 2, 29)));
    }

    @Test
    @DisplayName("Every day of a year gets a season and the year runs through all four in order")
    void everyDayOfTheYearGetsASeason() {
        LocalDate cursor = LocalDate.of(2026, 1, 1);
        Season previous = this.strategy.seasonAt(cursor);
        int changes = 0;

        while (cursor.getYear() == 2026) {
            Season season = this.strategy.seasonAt(cursor);
            if (season != previous) {
                assertSame(previous.next(), season, "seasons must follow each other in calendar order on " + cursor);
                changes++;
            }
            previous = season;
            cursor = cursor.plusDays(1);
        }

        assertEquals(4, changes, "a calendar year that starts and ends in winter switches four times");
    }

    @Test
    @DisplayName("The strategy is stateless and shared")
    void theStrategyIsShared() {
        assertSame(MeteorologicalSeasonStrategy.instance(), SeasonBoundaryStrategy.meteorological());
    }
}
