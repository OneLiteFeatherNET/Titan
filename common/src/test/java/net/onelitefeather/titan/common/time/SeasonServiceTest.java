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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static net.onelitefeather.titan.common.time.FixedInstants.BERLIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The service that provides the current season as state, driven by an injected clock.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
class SeasonServiceTest {

    /** A hot afternoon in the middle of August. */
    private static final Instant AUGUST = Instant.parse("2026-08-15T12:00:00Z");

    @Test
    @DisplayName("The default is the meteorological boundaries in the editorial zone")
    void theDefaultIsMeteorologicalInTheEditorialZone() {
        SeasonService service = SeasonService.create(Clock.fixed(AUGUST, BERLIN));

        assertSame(SeasonBoundaryStrategy.meteorological(), service.strategy());
        assertEquals(TitanTime.EDITORIAL_ZONE, service.zone());
        assertEquals(Season.SUMMER, service.currentSeason());
        assertEquals(LocalDate.of(2026, 8, 15), service.currentDate());
    }

    @Test
    @DisplayName("Winter can be tested in summer, because the clock and the boundaries are both injected")
    void winterCanBeTestedInSummer() {
        SeasonService service = SeasonService.create(Clock.fixed(AUGUST, BERLIN), SeasonBoundaryStrategy.fixed(Season.WINTER));

        assertEquals(Season.WINTER, service.currentSeason());
        assertEquals(LocalDate.of(2026, 8, 15), service.currentDate(), "pinning the season must not move the calendar");
    }

    @Test
    @DisplayName("A December clock gives winter without anyone waiting for December")
    void aDecemberClockGivesWinter() {
        Instant december = Instant.parse("2026-12-05T09:00:00Z");
        SeasonService service = SeasonService.create(Clock.fixed(december, BERLIN));

        assertEquals(Season.WINTER, service.currentSeason());
    }

    @Test
    @DisplayName("The zone decides the date, and with it the season on a boundary night")
    void theZoneDecidesTheDate() {
        // 23:30 UTC on the last day of February is already the first of March in Berlin.
        Instant boundaryNight = Instant.parse("2026-02-28T23:30:00Z");

        SeasonService berlin = SeasonService.create(Clock.fixed(boundaryNight, BERLIN));
        SeasonService utc = SeasonService.create(
                Clock.fixed(boundaryNight, ZoneOffset.UTC), ZoneOffset.UTC, SeasonBoundaryStrategy.meteorological());

        assertEquals(LocalDate.of(2026, 3, 1), berlin.currentDate());
        assertEquals(Season.SPRING, berlin.currentSeason());
        assertEquals(LocalDate.of(2026, 2, 28), utc.currentDate());
        assertEquals(Season.WINTER, utc.currentSeason());
    }

    @Test
    @DisplayName("The astronomical boundaries can be swapped in without touching the caller")
    void theAstronomicalBoundariesCanBeSwappedIn() {
        Instant midMarch = Instant.parse("2026-03-10T12:00:00Z");

        SeasonService meteorological = SeasonService.create(Clock.fixed(midMarch, BERLIN));
        SeasonService astronomical = SeasonService.create(
                Clock.fixed(midMarch, BERLIN), SeasonBoundaryStrategy.astronomical(BERLIN));

        assertEquals(Season.SPRING, meteorological.currentSeason());
        assertEquals(Season.WINTER, astronomical.currentSeason());
    }
}
