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

import net.onelitefeather.titan.common.time.season.AstronomicalSeasonStrategy.Event;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The astronomical boundaries, held against published equinox and solstice times.
 *
 * <p>The tolerance is three minutes: wide enough for the algorithm's own error and for a reference
 * that is quoted to the minute, narrow enough that a wrong periodic term or a missing &#916;T
 * correction shows up rather than hiding inside it.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
class AstronomicalSeasonStrategyTest {

    private static final Duration TOLERANCE = Duration.ofMinutes(3);

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private final SeasonBoundaryStrategy strategy = SeasonBoundaryStrategy.astronomical(BERLIN);

    @ParameterizedTest(name = "{0} {1} is {2}")
    @CsvSource({
            // Published times, in UTC.
            "2024, MARCH_EQUINOX,     2024-03-20T03:06:00Z", "2024, JUNE_SOLSTICE,     2024-06-20T20:51:00Z", "2024, SEPTEMBER_EQUINOX, 2024-09-22T12:44:00Z", "2024, DECEMBER_SOLSTICE, 2024-12-21T09:21:00Z", "2025, MARCH_EQUINOX,     2025-03-20T09:01:00Z", "2025, JUNE_SOLSTICE,     2025-06-21T02:42:00Z", "2025, SEPTEMBER_EQUINOX, 2025-09-22T18:19:00Z", "2025, DECEMBER_SOLSTICE, 2025-12-21T15:03:00Z", "2026, MARCH_EQUINOX,     2026-03-20T14:46:00Z", "2026, JUNE_SOLSTICE,     2026-06-21T08:25:00Z", "2026, SEPTEMBER_EQUINOX, 2026-09-23T00:05:00Z", "2026, DECEMBER_SOLSTICE, 2026-12-21T20:50:00Z",
    })
    @DisplayName("The event instants match the published equinox and solstice times")
    void theEventInstantsMatchPublishedTimes(int year, Event event, Instant expected) {
        Instant actual = AstronomicalSeasonStrategy.eventInstant(year, event);

        Duration off = Duration.between(expected, actual).abs();
        assertTrue(off.compareTo(TOLERANCE) <= 0, year + " " + event + ": expected around " + expected + " but was " + actual + " (" + off.toSeconds() + "s off)");
    }

    @ParameterizedTest(name = "{0} is {1}")
    @CsvSource({"2026-01-10, WINTER", "2026-03-19, WINTER", "2026-03-20, SPRING", "2026-06-20, SPRING", "2026-06-21, SUMMER", "2026-09-22, SUMMER", "2026-09-23, AUTUMN", "2026-12-20, AUTUMN", "2026-12-21, WINTER", "2026-12-31, WINTER",
    })
    @DisplayName("A season starts on the calendar day of its event, read in the configured zone")
    void aSeasonStartsOnTheDayOfItsEvent(LocalDate date, Season expected) {
        assertEquals(expected, this.strategy.seasonAt(date));
    }

    @Test
    @DisplayName("The events move between years, which is why they cannot be hard-coded dates")
    void theEventsMoveBetweenYears() {
        AstronomicalSeasonStrategy berlin = AstronomicalSeasonStrategy.of(BERLIN);

        // The December solstice falls on the 21st in 2026 and on the 22nd in 2027.
        assertEquals(LocalDate.of(2026, 12, 21), berlin.eventDate(2026, Event.DECEMBER_SOLSTICE));
        assertEquals(LocalDate.of(2027, 12, 22), berlin.eventDate(2027, Event.DECEMBER_SOLSTICE));
        // And the June solstice moves from the 21st to the 20th between 2026 and 2028.
        assertEquals(LocalDate.of(2026, 6, 21), berlin.eventDate(2026, Event.JUNE_SOLSTICE));
        assertEquals(LocalDate.of(2028, 6, 20), berlin.eventDate(2028, Event.JUNE_SOLSTICE));

        assertEquals(Season.AUTUMN, this.strategy.seasonAt(LocalDate.of(2027, 12, 21)));
        assertEquals(Season.WINTER, this.strategy.seasonAt(LocalDate.of(2027, 12, 22)));
    }

    @Test
    @DisplayName("The zone decides which calendar day an event lands on")
    void theZoneDecidesTheCalendarDay() {
        // The 2026 September equinox is 00:05 UTC on the 23rd, which is 02:05 on the 23rd in Berlin
        // but still the 22nd in New York.
        AstronomicalSeasonStrategy newYork = AstronomicalSeasonStrategy.of(ZoneId.of("America/New_York"));

        assertEquals(LocalDate.of(2026, 9, 23), AstronomicalSeasonStrategy.of(BERLIN).eventDate(2026, Event.SEPTEMBER_EQUINOX));
        assertEquals(LocalDate.of(2026, 9, 22), newYork.eventDate(2026, Event.SEPTEMBER_EQUINOX));
    }

    @Test
    @DisplayName("The astronomical boundaries lag the meteorological ones by about three weeks")
    void theBoundariesLagTheMeteorologicalOnes() {
        SeasonBoundaryStrategy meteorological = SeasonBoundaryStrategy.meteorological();
        LocalDate midMarch = LocalDate.of(2026, 3, 10);

        assertEquals(Season.SPRING, meteorological.seasonAt(midMarch));
        assertEquals(Season.WINTER, this.strategy.seasonAt(midMarch));
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
}
