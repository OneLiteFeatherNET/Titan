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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static net.onelitefeather.titan.common.time.FixedInstants.BERLIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That the configured strategy is the one that answers — not merely that the file was read.
 *
 * <p>Every test here goes through the two {@code create…} methods the application itself calls, and
 * then asks the resulting service a question whose answer differs per strategy. Asserting the
 * parsed string, or the class of {@code service.strategy()}, would pass just as happily if nobody
 * ever wired the provider into the lobby; a season that comes back as winter in March only comes
 * back that way if the astronomical boundaries really ran.
 *
 * <p>Two dates carry that weight:
 *
 * <ul>
 * <li>5 March 2026 — meteorological spring since the first of the month, astronomically still
 * winter until the equinox on the 20th.
 * <li>21 December 2026, 07:30 in Berlin — the sun rises at about 08:15, so the solar mapping is
 * still in the night half while the linear mapping is an hour and a half into its day.
 * </ul>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
class TimeConfigProviderTest {

    /** Meteorologically spring, astronomically still winter. */
    private static final LocalDate BEFORE_THE_MARCH_EQUINOX = LocalDate.of(2026, 3, 5);

    /** Summer under both sets of boundaries, so a fixed winter can only come from the pin. */
    private static final LocalDate MIDSUMMER = LocalDate.of(2026, 7, 15);

    /** Before sunrise in Berlin on the shortest day, but well after the linear daybreak. */
    private static final Instant DECEMBER_MORNING = LocalDateTime.of(FixedInstants.DECEMBER_SOLSTICE, LocalTime.of(7, 30)).atZone(BERLIN).toInstant();

    /** What {@link LinearDayTimeStrategy} makes of 07:30: 90 minutes past daybreak. */
    private static final long LINEAR_TICKS_AT_HALF_PAST_SEVEN = 1_500L;

    @Test
    @DisplayName("Without a file the lobby maps time linearly and reads meteorological seasons")
    void defaultsToLinearAndMeteorological(@TempDir Path directory) {
        TimeConfigProvider provider = TimeConfigProvider.create(directory, BERLIN);

        assertEquals(LINEAR_TICKS_AT_HALF_PAST_SEVEN, provider.createWorldTimeService(at(DECEMBER_MORNING)).currentTicks(), "the linear mapping is already in its day at 07:30");
        assertEquals(Season.SPRING, provider.createSeasonService(at(BEFORE_THE_MARCH_EQUINOX)).currentSeason(), "5 March is meteorological spring");
    }

    @Test
    @DisplayName("Without a file the defaults are written out, keys and all")
    void writesTheDefaultFile(@TempDir Path directory) throws IOException {
        TimeConfigProvider.create(directory, BERLIN);

        Path file = directory.resolve(TimeConfig.TIME_FILE_NAME);
        assertTrue(Files.exists(file), "the default file is written so an operator can see the keys");
        String written = Files.readString(file);
        assertTrue(written.contains(TimeConfig.LINEAR), () -> "expected the linear default in " + written);
        assertTrue(written.contains(TimeConfig.METEOROLOGICAL), () -> "expected the meteorological default in " + written);
    }

    @Test
    @DisplayName("An empty object is an absent choice and keeps both defaults")
    void emptyObjectKeepsTheDefaults(@TempDir Path directory) throws IOException {
        write(directory, "{}");

        TimeConfigProvider provider = TimeConfigProvider.create(directory, BERLIN);

        assertEquals(LINEAR_TICKS_AT_HALF_PAST_SEVEN, provider.createWorldTimeService(at(DECEMBER_MORNING)).currentTicks());
        assertEquals(Season.SPRING, provider.createSeasonService(at(BEFORE_THE_MARCH_EQUINOX)).currentSeason());
    }

    @Test
    @DisplayName("With the solar mapping configured the December morning is still night (US-2.07)")
    void solarMappingDrivesTheDayTime(@TempDir Path directory) throws IOException {
        write(directory, "{\"dayTimeStrategy\": \"solar\"}");

        long ticks = TimeConfigProvider.create(directory, BERLIN).createWorldTimeService(at(DECEMBER_MORNING)).currentTicks();

        assertTrue(ticks >= DayTimeStrategy.DUSK_TICK, () -> "the sun has not risen in Berlin at 07:30 on 21 December, but the world says tick " + ticks);
        // The same instant under the default, so the difference is the configuration and not the date.
        assertEquals(LINEAR_TICKS_AT_HALF_PAST_SEVEN, TimeConfigProvider.create(Files.createTempDirectory(directory, "linear"), BERLIN).createWorldTimeService(at(DECEMBER_MORNING)).currentTicks());
    }

    @Test
    @DisplayName("With the astronomical boundaries configured 5 March is still winter (US-2.12)")
    void astronomicalBoundariesDriveTheSeason(@TempDir Path directory) throws IOException {
        write(directory, "{\"seasonStrategy\": \"astronomical\"}");

        Season season = TimeConfigProvider.create(directory, BERLIN).createSeasonService(at(BEFORE_THE_MARCH_EQUINOX)).currentSeason();

        assertEquals(Season.WINTER, season, "the March equinox is on the 20th, so the 5th is still winter");
        // The same date under the default, so the difference is the configuration and not the date.
        assertEquals(Season.SPRING, TimeConfigProvider.create(Files.createTempDirectory(directory, "meteorological"), BERLIN).createSeasonService(at(BEFORE_THE_MARCH_EQUINOX)).currentSeason());
    }

    @Test
    @DisplayName("With a season pinned the lobby answers winter in July (US-2.13)")
    void fixedSeasonIgnoresTheCalendar(@TempDir Path directory) throws IOException {
        write(directory, "{\"seasonStrategy\": \"fixed\", \"fixedSeason\": \"winter\"}");

        Season season = TimeConfigProvider.create(directory, BERLIN).createSeasonService(at(MIDSUMMER)).currentSeason();

        assertEquals(Season.WINTER, season, "a pinned season does not consult the date");
        // Both boundary rules would say summer here, so the pin is the only thing that can answer winter.
        assertEquals(Season.SUMMER, TimeConfigProvider.create(Files.createTempDirectory(directory, "meteorological"), BERLIN).createSeasonService(at(MIDSUMMER)).currentSeason());
    }

    @Test
    @DisplayName("A pinned season survives a strategy switch and is ignored while it is not asked for")
    void fixedSeasonIsIgnoredByTheOtherStrategies(@TempDir Path directory) throws IOException {
        write(directory, "{\"seasonStrategy\": \"meteorological\", \"fixedSeason\": \"winter\"}");

        assertEquals(Season.SUMMER, TimeConfigProvider.create(directory, BERLIN).createSeasonService(at(MIDSUMMER)).currentSeason());
    }

    @Test
    @DisplayName("Strategy names are read case- and space-insensitively")
    void namesAreNormalized(@TempDir Path directory) throws IOException {
        write(directory, "{\"dayTimeStrategy\": \"  Solar \", \"seasonStrategy\": \"FIXED\", \"fixedSeason\": \"Winter\"}");

        TimeConfigProvider provider = TimeConfigProvider.create(directory, BERLIN);

        assertTrue(provider.createWorldTimeService(at(DECEMBER_MORNING)).currentTicks() >= DayTimeStrategy.DUSK_TICK);
        assertEquals(Season.WINTER, provider.createSeasonService(at(MIDSUMMER)).currentSeason());
    }

    @Test
    @DisplayName("An unknown day time strategy stops the boot and names the value and the valid ones")
    void unknownDayTimeStrategyFails(@TempDir Path directory) throws IOException {
        write(directory, "{\"dayTimeStrategy\": \"astronomical\"}");

        TimeConfigException exception = assertThrows(TimeConfigException.class, () -> TimeConfigProvider.create(directory, BERLIN));

        assertMentions(exception, "dayTimeStrategy", "astronomical", TimeConfig.LINEAR, TimeConfig.SOLAR);
    }

    @Test
    @DisplayName("An unknown season strategy stops the boot and names the value and the valid ones")
    void unknownSeasonStrategyFails(@TempDir Path directory) throws IOException {
        write(directory, "{\"seasonStrategy\": \"solar\"}");

        TimeConfigException exception = assertThrows(TimeConfigException.class, () -> TimeConfigProvider.create(directory, BERLIN));

        assertMentions(exception, "seasonStrategy", "solar", TimeConfig.METEOROLOGICAL, TimeConfig.ASTRONOMICAL, TimeConfig.FIXED);
    }

    @Test
    @DisplayName("An unknown season name stops the boot and names the value and the four seasons")
    void unknownFixedSeasonFails(@TempDir Path directory) throws IOException {
        write(directory, "{\"seasonStrategy\": \"fixed\", \"fixedSeason\": \"wintre\"}");

        TimeConfigException exception = assertThrows(TimeConfigException.class, () -> TimeConfigProvider.create(directory, BERLIN));

        assertMentions(exception, "fixedSeason", "wintre", "spring", "summer", "autumn", "winter");
    }

    @Test
    @DisplayName("A pinned season strategy without a season stops the boot")
    void fixedWithoutSeasonFails(@TempDir Path directory) throws IOException {
        write(directory, "{\"seasonStrategy\": \"fixed\"}");

        TimeConfigException exception = assertThrows(TimeConfigException.class, () -> TimeConfigProvider.create(directory, BERLIN));

        assertMentions(exception, "fixedSeason", TimeConfig.FIXED, "spring", "summer", "autumn", "winter");
    }

    @Test
    @DisplayName("A blank value is a mistake, not an absent key")
    void blankValueFails(@TempDir Path directory) throws IOException {
        write(directory, "{\"seasonStrategy\": \"   \"}");

        assertThrows(TimeConfigException.class, () -> TimeConfigProvider.create(directory, BERLIN));
    }

    @Test
    @DisplayName("A file that is not JSON stops the boot instead of running an unchosen strategy")
    void malformedFileFails(@TempDir Path directory) throws IOException {
        write(directory, "{\"seasonStrategy\": ");

        TimeConfigException exception = assertThrows(TimeConfigException.class, () -> TimeConfigProvider.create(directory, BERLIN));

        assertMentions(exception, TimeConfig.TIME_FILE_NAME);
    }

    private static void assertMentions(TimeConfigException exception, String... expected) {
        String message = exception.getMessage();
        for (String fragment : expected) {
            assertTrue(message.contains(fragment), () -> "expected \"" + fragment + "\" in: " + message);
        }
    }

    private static void write(Path directory, String json) throws IOException {
        Files.writeString(directory.resolve(TimeConfig.TIME_FILE_NAME), json);
    }

    private static Clock at(Instant instant) {
        return Clock.fixed(instant, BERLIN);
    }

    private static Clock at(LocalDate date) {
        return at(date.atTime(LocalTime.NOON).atZone(BERLIN).toInstant());
    }
}
