/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.feature.daytime;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DayTimeMappingTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final long TICKS_PER_DAY = 24000L;

    private final DayTimeMapping mapping = new DayTimeMapping();

    private long ticksInBerlin(String isoInstant) {
        return this.mapping.ticksAt(Instant.parse(isoInstant), BERLIN);
    }

    @DisplayName("06:00 is sunrise at tick 0")
    @Test
    void sunriseIsTickZero() {
        Assertions.assertEquals(0L, ticksInBerlin("2026-01-15T05:00:00Z"), "06:00 CET");
    }

    @DisplayName("12:00 is noon at tick 6000")
    @Test
    void noonIsTick6000() {
        Assertions.assertEquals(6000L, ticksInBerlin("2026-01-15T11:00:00Z"), "12:00 CET");
    }

    @DisplayName("18:00 is sunset at tick 12000")
    @Test
    void sunsetIsTick12000() {
        Assertions.assertEquals(12000L, ticksInBerlin("2026-01-15T17:00:00Z"), "18:00 CET");
    }

    @DisplayName("00:00 is midnight at tick 18000")
    @Test
    void midnightIsTick18000() {
        Assertions.assertEquals(18000L, ticksInBerlin("2026-01-14T23:00:00Z"), "00:00 CET");
    }

    @DisplayName("09:00 is tick 3000")
    @Test
    void nineOClockIsTick3000() {
        Assertions.assertEquals(3000L, ticksInBerlin("2026-01-15T08:00:00Z"), "09:00 CET");
    }

    @DisplayName("The zone decides: 06:00 in Tokyo is tick 0 whatever time it is in Berlin")
    @Test
    void tokyoSunriseIsTickZero() {
        Instant tokyoSix = Instant.parse("2026-01-14T21:00:00Z");

        Assertions.assertEquals(0L, this.mapping.ticksAt(tokyoSix, TOKYO), "06:00 in Tokyo");
        Assertions.assertNotEquals(0L, this.mapping.ticksAt(tokyoSix, BERLIN), "it is 22:00 in Berlin then, so the zone must matter");
    }

    @DisplayName("Spring forward: the wall clock jumps from 01:59:59 to 03:00:00 and the ticks jump forward with it")
    @Test
    void springForwardJumpsForward() {
        Assertions.assertEquals(19999L, ticksInBerlin("2026-03-29T00:59:59Z"), "01:59:59 CET");
        Assertions.assertEquals(21000L, ticksInBerlin("2026-03-29T01:00:00Z"), "03:00:00 CEST");
    }

    @DisplayName("Fall back: the repeated 02:00 hour maps to the same ticks as the first one")
    @Test
    void fallBackRepeatsTheWallClockValue() {
        Assertions.assertEquals(20000L, ticksInBerlin("2026-10-25T00:00:00Z"), "02:00 CEST, before the change");
        Assertions.assertEquals(20000L, ticksInBerlin("2026-10-25T01:00:00Z"), "02:00 CET, after the change");
    }

    @DisplayName("Summer solstice: 12:00 Berlin time is tick 6000")
    @Test
    void summerSolsticeNoon() {
        Assertions.assertEquals(6000L, ticksInBerlin("2026-06-21T10:00:00Z"), "12:00 CEST");
    }

    @DisplayName("Winter solstice: 12:00 Berlin time is tick 6000")
    @Test
    void winterSolsticeNoon() {
        Assertions.assertEquals(6000L, ticksInBerlin("2026-12-21T11:00:00Z"), "12:00 CET");
    }

    @DisplayName("Every sampled moment of the year stays in [0, 24000)")
    @Test
    void resultStaysWithinOneMinecraftDay() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        for (int minute = 0; minute < 366 * 24 * 60; minute += 7) {
            long ticks = this.mapping.ticksAt(start.plus(minute, ChronoUnit.MINUTES), BERLIN);

            Assertions.assertTrue(ticks >= 0 && ticks < TICKS_PER_DAY, "ticks must be in [0, 24000) but were " + ticks + " at minute " + minute);
        }
    }

    @DisplayName("The same instant and zone always give the same ticks")
    @Test
    void mappingIsDeterministic() {
        Instant instant = Instant.parse("2026-05-05T12:34:56Z");

        Assertions.assertEquals(this.mapping.ticksAt(instant, BERLIN), new DayTimeMapping().ticksAt(instant, BERLIN), "two mappings must agree");
    }

    @DisplayName("Across the spring-forward night the ticks never move backwards")
    @Test
    void neverMovesBackwardsAcrossSpringForward() {
        List<Long> samples = sampleEveryMinute(Instant.parse("2026-03-28T22:00:00Z"), 4 * 60);

        Assertions.assertEquals(0, stepsBackwards(samples), "no backwards step between 23:00 and 04:00");
    }

    @DisplayName("Over 24 hours the ticks wrap around exactly once")
    @Test
    void wrapsExactlyOncePerDay() {
        List<Long> samples = sampleEveryMinute(Instant.parse("2026-01-15T00:00:00Z"), 24 * 60);

        Assertions.assertEquals(1, stepsBackwards(samples), "one sunrise per 24 hours");
    }

    private List<Long> sampleEveryMinute(Instant start, int minutes) {
        List<Long> samples = new ArrayList<>();
        for (int minute = 0; minute <= minutes; minute++) {
            samples.add(this.mapping.ticksAt(start.plus(minute, ChronoUnit.MINUTES), BERLIN));
        }
        return samples;
    }

    private static int stepsBackwards(List<Long> samples) {
        int steps = 0;
        for (int i = 1; i < samples.size(); i++) {
            if (samples.get(i) < samples.get(i - 1)) {
                steps++;
            }
        }
        return steps;
    }
}
