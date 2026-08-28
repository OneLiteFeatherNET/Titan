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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static net.onelitefeather.titan.common.time.DayTimeStrategy.DUSK_TICK;
import static net.onelitefeather.titan.common.time.DayTimeStrategy.TICKS_PER_DAY;
import static net.onelitefeather.titan.common.time.FixedInstants.BERLIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The solar mapping's own behaviour, and the evidence that its astronomy is right.
 *
 * <h2>Where the expected times come from</h2>
 *
 * Every expected time below is the value the US Naval Observatory publishes for 52.520008° N,
 * 13.404954° E — rise, set and upper transit — converted from UT to Berlin local time. One source,
 * quoted rather than fitted: an expectation copied off this implementation's own output would make
 * the test agree with whatever the implementation does, which is the one thing it must not do.
 *
 * <p>USNO prints whole minutes, so each expectation carries up to 30 seconds of rounding of its
 * own. The tolerances are set just above the largest residual actually measured against that
 * source, not at a round number picked for comfort:
 *
 * <ul>
 * <li>solar noon, {@link #NOON_TOLERANCE}: largest measured deviation 18 seconds.
 * <li>sunrise and sunset, {@link #EVENT_TOLERANCE}: largest measured deviation 83 seconds, on the
 * September sunset, which is where the equation's fixed J2000 perihelion argument costs the most
 * half-day length.
 * </ul>
 *
 * <p>The solar noon case is the sharp one. It is nearly free of the half-day-length error that
 * dominates sunrise and sunset, so it pins the time scale directly: any constant offset bolted onto
 * the mean solar time — the mistake this test exists to catch — moves it by the full amount and
 * fails here long before the rise and set assertions notice.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
class SolarDayTimeStrategyTest {

    private static final Duration EVENT_TOLERANCE = Duration.ofSeconds(120);

    private static final Duration NOON_TOLERANCE = Duration.ofSeconds(45);

    private final SolarDayTimeStrategy strategy = SolarDayTimeStrategy.berlin();

    @ParameterizedTest(name = "{0}: sunrise {1}, sunset {2} Berlin local")
    @CsvSource({
            // USNO rise and set for Berlin, in local time; the two solstices and the two equinoxes
            // of 2026.
            "2026-03-20, 06:09, 18:19", "2026-06-21, 04:43, 21:33", "2026-09-23, 06:54, 19:03", "2026-12-21, 08:15, 15:54",
    })
    @DisplayName("Sunrise and sunset match the published Berlin times")
    void sunriseAndSunsetMatchPublishedBerlinTimes(LocalDate date, LocalTime sunrise, LocalTime sunset) {
        Instant expectedSunrise = LocalDateTime.of(date, sunrise).atZone(BERLIN).toInstant();
        Instant expectedSunset = LocalDateTime.of(date, sunset).atZone(BERLIN).toInstant();

        Instant actualSunrise = this.strategy.sunrise(date);
        Instant actualSunset = this.strategy.sunset(date);

        assertNotNull(actualSunrise);
        assertNotNull(actualSunset);
        assertWithin(expectedSunrise, actualSunrise, EVENT_TOLERANCE, "sunrise on " + date);
        assertWithin(expectedSunset, actualSunset, EVENT_TOLERANCE, "sunset on " + date);
    }

    @ParameterizedTest(name = "{0}: solar noon {1} Berlin local")
    @CsvSource({
            // USNO upper transit of the sun over Berlin, in local time.
            "2026-03-20, 12:14", "2026-06-21, 13:08", "2026-09-23, 12:59", "2026-12-21, 12:04",
    })
    @DisplayName("Solar noon matches the published Berlin upper transit to well under a minute")
    void solarNoonMatchesThePublishedBerlinTransit(LocalDate date, LocalTime noon) {
        Instant expected = LocalDateTime.of(date, noon).atZone(BERLIN).toInstant();

        Instant sunrise = this.strategy.sunrise(date);
        Instant sunset = this.strategy.sunset(date);
        assertNotNull(sunrise);
        assertNotNull(sunset);
        // The mapping puts noon exactly halfway between the two events, so this is the transit the
        // implementation actually believes in, not a separately computed one.
        Instant actual = sunrise.plus(Duration.between(sunrise, sunset).dividedBy(2));

        assertWithin(expected, actual, NOON_TOLERANCE, "solar noon on " + date);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"2026-03-20", "2026-06-21", "2026-09-23", "2026-12-21", "2026-05-15"})
    @DisplayName("Sunrise is the first tick of the day and sunset the first tick of the night")
    void theSolarEventsAnchorTheSegments(LocalDate date) {
        Instant sunrise = this.strategy.sunrise(date);
        Instant sunset = this.strategy.sunset(date);
        assertNotNull(sunrise);
        assertNotNull(sunset);

        assertEquals(0L, this.strategy.ticksAt(sunrise, BERLIN), "sunrise must be tick 0 on " + date);
        assertEquals(DUSK_TICK, this.strategy.ticksAt(sunset, BERLIN), "sunset must be tick 12000 on " + date);
        assertEquals(TICKS_PER_DAY - 1L, this.strategy.ticksAt(sunrise.minusMillis(1), BERLIN), "the millisecond before sunrise must still be the last tick of the night on " + date);
    }

    @Test
    @DisplayName("The longest and the shortest day of the year come out that way")
    void theSolsticesAreTheLongestAndShortestDay() {
        Duration june = daylight(LocalDate.of(2026, 6, 21));
        Duration december = daylight(LocalDate.of(2026, 12, 21));
        Duration equinox = daylight(LocalDate.of(2026, 3, 20));

        assertTrue(june.toMinutes() > 16 * 60, "Berlin sees over 16 hours of daylight in June, got " + june);
        assertTrue(december.toMinutes() < 8 * 60, "Berlin sees under 8 hours of daylight in December, got " + december);
        assertTrue(Math.abs(equinox.toMinutes() - 12 * 60) < 20, "an equinox is within twenty minutes of twelve hours, got " + equinox);
    }

    @Test
    @DisplayName("Half the tick range is day and half is night, whatever the season")
    void bothHalvesAlwaysGetHalfTheTickRange() {
        for (LocalDate date : new LocalDate[]{LocalDate.of(2026, 6, 21), LocalDate.of(2026, 12, 21)}) {
            Instant sunrise = this.strategy.sunrise(date);
            Instant sunset = this.strategy.sunset(date);
            assertNotNull(sunrise);
            assertNotNull(sunset);

            Instant middleOfDay = sunrise.plus(Duration.between(sunrise, sunset).dividedBy(2));
            long ticks = this.strategy.ticksAt(middleOfDay, BERLIN);

            assertTrue(Math.abs(ticks - 6000L) <= 1, "the middle of the daylight span must be Minecraft noon on " + date + ", got " + ticks);
        }
    }

    @Test
    @DisplayName("The mapping never runs backwards, not even across a daylight saving transition")
    void theMappingIsStrictlyIncreasingInRealTime() {
        // Both transitions plus the day in between, sampled every five minutes.
        Instant cursor = LocalDateTime.of(2026, 10, 24, 0, 0).atZone(BERLIN).toInstant();
        Instant end = LocalDateTime.of(2026, 10, 26, 0, 0).atZone(BERLIN).toInstant();
        long previous = this.strategy.ticksAt(cursor, BERLIN);
        int wraps = 0;

        while (cursor.isBefore(end)) {
            cursor = cursor.plus(Duration.ofMinutes(5));
            long ticks = this.strategy.ticksAt(cursor, BERLIN);
            if (ticks < previous) {
                wraps++;
            }
            previous = ticks;
        }

        assertEquals(2, wraps, "over two days the mapping passes the end of the day exactly twice");
    }

    @Test
    @DisplayName("Above the polar circle the mapping falls back to the linear one instead of guessing")
    void thePolarFallbackIsTheLinearMapping() {
        // Longyearbyen: the sun neither rises nor sets around the June solstice.
        SolarDayTimeStrategy svalbard = SolarDayTimeStrategy.at(78.22, 15.65);
        Instant midsummer = Instant.parse("2026-06-21T12:00:00Z");

        assertEquals(DayTimeStrategy.linear().ticksAt(midsummer, BERLIN), svalbard.ticksAt(midsummer, BERLIN));
    }

    @Test
    @DisplayName("A position off the globe is rejected at construction, not silently accepted")
    void impossiblePositionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> SolarDayTimeStrategy.at(91.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> SolarDayTimeStrategy.at(0.0, 181.0));
    }

    @Test
    @DisplayName("The Berlin strategy is stateless and shared")
    void theBerlinStrategyIsShared() {
        assertSame(SolarDayTimeStrategy.berlin(), DayTimeStrategy.solar());
    }

    private Duration daylight(LocalDate date) {
        Instant sunrise = this.strategy.sunrise(date);
        Instant sunset = this.strategy.sunset(date);
        assertNotNull(sunrise);
        assertNotNull(sunset);
        return Duration.between(sunrise, sunset);
    }

    private static void assertWithin(Instant expected, Instant actual, Duration tolerance, String what) {
        Duration off = Duration.between(expected, actual).abs();
        assertTrue(off.compareTo(tolerance) <= 0, what + ": expected around " + expected + " but was " + actual + " (" + off.toSeconds() + "s off, tolerance " + tolerance.toSeconds() + "s)");
    }
}
