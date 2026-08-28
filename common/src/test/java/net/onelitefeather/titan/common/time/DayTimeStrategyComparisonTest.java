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

import net.onelitefeather.titan.common.time.FixedInstants.Phase;
import net.onelitefeather.titan.common.time.FixedInstants.Sample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static net.onelitefeather.titan.common.time.DayTimeStrategy.DUSK_TICK;
import static net.onelitefeather.titan.common.time.DayTimeStrategy.TICKS_PER_DAY;
import static net.onelitefeather.titan.common.time.FixedInstants.BERLIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Holds both day-time strategies against the same set of fixed instants (US-2.08).
 *
 * <p>This is the test the strategy pattern exists for. A configuration flag would have made these
 * two mappings two branches of one method, and two branches cannot be handed the same fixture and
 * compared. Everything asserted here is a property both mappings owe the lobby regardless of how
 * they compute it; where they legitimately differ,
 * {@link #linearFollowsTheWallClockWhereSolarDoesNot()}
 * pins the difference down instead of leaving it implied.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
class DayTimeStrategyComparisonTest {

    static Stream<DayTimeStrategy> strategies() {
        return Stream.of(DayTimeStrategy.linear(), DayTimeStrategy.solar());
    }

    static Stream<Arguments> strategiesAndSamples() {
        List<Sample> samples = FixedInstants.all();
        return strategies().flatMap(strategy -> samples.stream().map(sample -> Arguments.of(strategy, sample)));
    }

    static Stream<Arguments> strategiesAndPhasedSamples() {
        List<Sample> samples = FixedInstants.all().stream().filter(sample -> sample.phase() != Phase.UNSPECIFIED).toList();
        return strategies().flatMap(strategy -> samples.stream().map(sample -> Arguments.of(strategy, sample)));
    }

    @ParameterizedTest(name = "{0} at {1}")
    @MethodSource("strategiesAndSamples")
    @DisplayName("Every strategy stays inside one Minecraft day at every fixed instant")
    void everyStrategyStaysInsideOneMinecraftDay(DayTimeStrategy strategy, Sample sample) {
        long ticks = strategy.ticksAt(sample.instant(), BERLIN);

        assertTrue(ticks >= 0, () -> strategy + " returned a negative tick at " + sample + ": " + ticks);
        assertTrue(ticks < TICKS_PER_DAY, () -> strategy + " returned a tick past the end of the day at " + sample + ": " + ticks);
    }

    @ParameterizedTest(name = "{0} at {1}")
    @MethodSource("strategiesAndSamples")
    @DisplayName("Every strategy answers the same instant with the same tick")
    void everyStrategyIsDeterministic(DayTimeStrategy strategy, Sample sample) {
        long first = strategy.ticksAt(sample.instant(), BERLIN);
        long second = strategy.ticksAt(sample.instant(), BERLIN);

        assertEquals(first, second, () -> strategy + " is not deterministic at " + sample);
    }

    @ParameterizedTest(name = "{0} at {1}")
    @MethodSource("strategiesAndPhasedSamples")
    @DisplayName("Every strategy agrees on whether the sun is up")
    void everyStrategyAgreesOnDayAndNight(DayTimeStrategy strategy, Sample sample) {
        long ticks = strategy.ticksAt(sample.instant(), BERLIN);

        if (sample.phase() == Phase.DAY) {
            assertTrue(ticks < DUSK_TICK, () -> strategy + " puts " + sample + " into the night half: " + ticks);
        } else {
            assertTrue(ticks >= DUSK_TICK, () -> strategy + " puts " + sample + " into the day half: " + ticks);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("strategies")
    @DisplayName("Every strategy survives the spring forward without leaving the day")
    void everyStrategySurvivesTheSpringForward(DayTimeStrategy strategy) {
        // Local time jumps from 01:59 CET straight to 03:01 CEST; two real minutes pass.
        Instant before = Instant.parse("2026-03-29T00:59:00Z");
        Instant after = Instant.parse("2026-03-29T01:01:00Z");

        long ticksBefore = strategy.ticksAt(before, BERLIN);
        long ticksAfter = strategy.ticksAt(after, BERLIN);

        assertTrue(ticksAfter > ticksBefore, () -> strategy + " went backwards across the spring forward: " + ticksBefore + " -> " + ticksAfter);
        assertTrue(ticksAfter < TICKS_PER_DAY && ticksBefore >= 0, () -> strategy + " left the day across the spring forward");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("strategies")
    @DisplayName("Every strategy is driven by the injected clock, never by the machine clock")
    void everyStrategyIsDrivenByTheInjectedClock(DayTimeStrategy strategy) {
        Instant instant = Instant.parse("2026-12-21T14:00:00Z");
        WorldTimeService service = WorldTimeService.create(Clock.fixed(instant, BERLIN), BERLIN, strategy);

        assertEquals(strategy.ticksAt(instant, BERLIN), service.currentTicks());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("strategies")
    @DisplayName("Every strategy advances over the course of an ordinary day")
    void everyStrategyAdvancesOverTheDay(DayTimeStrategy strategy) {
        Instant start = Instant.parse("2026-05-15T04:00:00Z");
        long previous = strategy.ticksAt(start, BERLIN);
        int wrapCount = 0;

        for (int minute = 10; minute <= 24 * 60; minute += 10) {
            long ticks = strategy.ticksAt(start.plus(Duration.ofMinutes(minute)), BERLIN);
            if (ticks < previous) {
                wrapCount++;
            }
            previous = ticks;
        }

        int wraps = wrapCount;
        assertEquals(1, wraps, () -> strategy + " passed the end of the day " + wraps + " times over 24 real hours");
    }

    @ParameterizedTest(name = "{0} at {1}")
    @MethodSource("strategiesAndSamples")
    @DisplayName("No strategy needs real time to pass")
    void noStrategyNeedsRealTime(DayTimeStrategy strategy, Sample sample) {
        Clock frozen = Clock.fixed(sample.instant(), BERLIN);
        WorldTimeService service = WorldTimeService.create(frozen, BERLIN, strategy);

        assertEquals(service.currentTicks(), service.currentTicks());
        assertEquals(strategy.ticksAt(sample.instant(), BERLIN), service.currentTicks());
    }

    @Test
    @DisplayName("Where the two mappings differ: the linear one follows the wall clock, the solar one the sun")
    void linearFollowsTheWallClockWhereSolarDoesNot() {
        DayTimeStrategy linear = DayTimeStrategy.linear();
        DayTimeStrategy solar = DayTimeStrategy.solar();

        // Two minutes of real time across the spring forward, during which the wall clock gains an
        // hour. The linear mapping is a function of the wall clock and jumps with it; the solar
        // mapping is a function of the instant and does not notice.
        Instant before = Instant.parse("2026-03-29T00:59:00Z");
        Instant after = Instant.parse("2026-03-29T01:01:00Z");

        long linearJump = linear.ticksAt(after, BERLIN) - linear.ticksAt(before, BERLIN);
        long solarJump = solar.ticksAt(after, BERLIN) - solar.ticksAt(before, BERLIN);

        // One hour is 1000 ticks, two real minutes are another 33.
        assertEquals(1033, linearJump, "the linear mapping must jump the skipped hour with the wall clock");
        assertTrue(solarJump < 60, "the solar mapping must not notice the wall clock at all, but moved " + solarJump);

        // And in December the difference is the point of the whole exercise: at eight in the morning
        // Berlin has not seen the sun yet, but the linear mapping has been in daylight for two hours.
        Instant decemberMorning = Instant.parse("2026-12-21T07:00:00Z");
        assertTrue(linear.ticksAt(decemberMorning, BERLIN) < DUSK_TICK, "the linear mapping calls 08:00 in December daytime");
        assertTrue(solar.ticksAt(decemberMorning, BERLIN) >= DUSK_TICK, "the solar mapping must still be in the night at 08:00 on the December solstice");
    }
}
