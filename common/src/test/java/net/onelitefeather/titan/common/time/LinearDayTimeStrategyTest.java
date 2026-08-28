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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static net.onelitefeather.titan.common.time.FixedInstants.BERLIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The linear mapping's own behaviour: the wall clock, and nothing but the wall clock.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
class LinearDayTimeStrategyTest {

    private final DayTimeStrategy strategy = DayTimeStrategy.linear();

    @ParameterizedTest(name = "{0}:{1} local is tick {2}")
    @CsvSource({"6, 0, 0", "12, 0, 6000", "18, 0, 12000", "0, 0, 18000", "9, 0, 3000", "6, 1, 16",
    })
    @DisplayName("Local wall clock maps evenly onto the Minecraft day, noon at noon")
    void wallClockMapsEvenlyOntoTheDay(int hour, int minute, long expected) {
        Instant instant = LocalDateTime.of(LocalDate.of(2026, 5, 15), LocalTime.of(hour, minute)).atZone(BERLIN).toInstant();

        assertEquals(expected, this.strategy.ticksAt(instant, BERLIN));
    }

    @Test
    @DisplayName("Noon is noon in winter and in summer, so daylight saving cannot shift the lobby")
    void noonIsNoonOnBothSidesOfDaylightSaving() {
        Instant winterNoon = LocalDateTime.of(2026, 1, 15, 12, 0).atZone(BERLIN).toInstant();
        Instant summerNoon = LocalDateTime.of(2026, 7, 15, 12, 0).atZone(BERLIN).toInstant();

        assertEquals(DayTimeStrategy.NOON_TICK, this.strategy.ticksAt(winterNoon, BERLIN));
        assertEquals(DayTimeStrategy.NOON_TICK, this.strategy.ticksAt(summerNoon, BERLIN));
        // The two instants are one UTC hour apart; only the zone makes them the same game time.
        assertNotEquals(this.strategy.ticksAt(winterNoon, ZoneOffset.UTC), this.strategy.ticksAt(summerNoon, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("The skipped hour in March is skipped in the lobby as well")
    void theSpringForwardSkipsAnHourOfGameTime() {
        // 01:59 CET, then two real minutes later 03:01 CEST.
        long before = this.strategy.ticksAt(Instant.parse("2026-03-29T00:59:00Z"), BERLIN);
        long after = this.strategy.ticksAt(Instant.parse("2026-03-29T01:01:00Z"), BERLIN);

        assertEquals(19983, before);
        assertEquals(21016, after);
    }

    @Test
    @DisplayName("The repeated hour in October is repeated in the lobby as well")
    void theFallBackRepeatsAnHourOfGameTime() {
        // 02:30 CEST and, an hour of real time later, 02:30 CET.
        long firstPass = this.strategy.ticksAt(Instant.parse("2026-10-25T00:30:00Z"), BERLIN);
        long secondPass = this.strategy.ticksAt(Instant.parse("2026-10-25T01:30:00Z"), BERLIN);

        assertEquals(firstPass, secondPass);
    }

    @Test
    @DisplayName("The zone handed in decides the result")
    void theZoneDecides() {
        Instant instant = Instant.parse("2026-05-15T10:00:00Z");

        assertEquals(DayTimeStrategy.NOON_TICK, this.strategy.ticksAt(instant, BERLIN));
        assertEquals(4000, this.strategy.ticksAt(instant, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("The strategy is stateless and shared")
    void theStrategyIsShared() {
        assertSame(LinearDayTimeStrategy.instance(), DayTimeStrategy.linear());
    }
}
