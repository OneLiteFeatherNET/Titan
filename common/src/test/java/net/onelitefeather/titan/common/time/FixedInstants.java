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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * The one set of fixed instants every day-time strategy is measured against (US-2.08).
 *
 * <p>The comparison between the linear and the solar mapping is only worth anything if both are
 * asked the same questions, so the questions live here and not in either strategy's own test: both
 * solstices, both equinoxes, both daylight saving transitions for {@code Europe/Berlin}, and an
 * ordinary day with nothing special about it.
 *
 * <p>Every instant is written as UTC. Local wall-clock time is ambiguous exactly on the two dates
 * that matter most here — 02:30 exists twice on 25 October 2026 and not at all on 29 March 2026 —
 * and an ambiguous fixture is a fixture that silently tests something else.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
final class FixedInstants {

    static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    /** The March equinox of 2026; day and night are the same length. */
    static final LocalDate MARCH_EQUINOX = LocalDate.of(2026, 3, 20);

    /** The June solstice of 2026; the longest day of the year in Berlin. */
    static final LocalDate JUNE_SOLSTICE = LocalDate.of(2026, 6, 21);

    /** The September equinox of 2026. */
    static final LocalDate SEPTEMBER_EQUINOX = LocalDate.of(2026, 9, 23);

    /** The December solstice of 2026; the shortest day of the year in Berlin. */
    static final LocalDate DECEMBER_SOLSTICE = LocalDate.of(2026, 12, 21);

    /** The last Sunday in March 2026: 02:00 CET becomes 03:00 CEST, and an hour never happens. */
    static final LocalDate DST_SPRING_FORWARD = LocalDate.of(2026, 3, 29);

    /** The last Sunday in October 2026: 03:00 CEST becomes 02:00 CET, and an hour happens twice. */
    static final LocalDate DST_FALL_BACK = LocalDate.of(2026, 10, 25);

    /** A Friday in May with no astronomical or civil event attached to it. */
    static final LocalDate ORDINARY_DAY = LocalDate.of(2026, 5, 15);

    private FixedInstants() {
    }

    /**
     * Returns the shared sample set.
     *
     * @return every fixed instant both strategies are checked against
     */
    static List<Sample> all() {
        List<Sample> samples = new ArrayList<>();
        for (LocalDate date : List.of(
                MARCH_EQUINOX, JUNE_SOLSTICE, SEPTEMBER_EQUINOX, DECEMBER_SOLSTICE, ORDINARY_DAY)) {
            samples.add(local(date, "midnight", 0, 0, Phase.NIGHT));
            samples.add(local(date, "morning", 6, 0, Phase.UNSPECIFIED));
            samples.add(local(date, "noon", 12, 0, Phase.DAY));
            samples.add(local(date, "evening", 18, 0, Phase.UNSPECIFIED));
        }

        // Daylight saving, spring: the local clock jumps from 01:59:59 CET to 03:00:00 CEST.
        samples.add(utc(DST_SPRING_FORWARD, "before spring forward", 0, 59, Phase.UNSPECIFIED));
        samples.add(utc(DST_SPRING_FORWARD, "after spring forward", 1, 1, Phase.UNSPECIFIED));
        samples.add(utc(DST_SPRING_FORWARD, "noon after spring forward", 10, 0, Phase.DAY));

        // Daylight saving, autumn: 02:30 local happens twice, first as CEST then as CET.
        samples.add(utc(DST_FALL_BACK, "first pass of the repeated hour", 0, 30, Phase.NIGHT));
        samples.add(utc(DST_FALL_BACK, "second pass of the repeated hour", 1, 30, Phase.NIGHT));
        samples.add(utc(DST_FALL_BACK, "noon after fall back", 11, 0, Phase.DAY));

        return List.copyOf(samples);
    }

    private static Sample local(LocalDate date, String label, int hour, int minute, Phase phase) {
        Instant instant = LocalDateTime.of(date, java.time.LocalTime.of(hour, minute)).atZone(BERLIN).toInstant();
        return new Sample(date + " " + label, instant, phase);
    }

    private static Sample utc(LocalDate date, String label, int hour, int minute, Phase phase) {
        Instant instant = LocalDateTime.of(date, java.time.LocalTime.of(hour, minute)).toInstant(ZoneOffset.UTC);
        return new Sample(date + " " + label, instant, phase);
    }

    /**
     * One point in the shared sample set.
     *
     * @param label   what the point is, for the test report
     * @param instant the instant itself
     * @param phase   what every strategy has to agree on at that instant
     */
    record Sample(String label, Instant instant, Phase phase) {

        @Override
        public String toString() {
            return this.label;
        }
    }

    /**
     * What both strategies must agree on at a sample point.
     *
     * <p>The two mappings differ by design in where exactly they put a given minute; they do not
     * get
     * to disagree about whether the sun is up.
     */
    enum Phase {

        /** The sun is up: the tick belongs to {@code [0, 12000)}. */
        DAY,

        /** The sun is down: the tick belongs to {@code [12000, 24000)}. */
        NIGHT,

        /** Near a transition, where the two mappings legitimately fall on different sides. */
        UNSPECIFIED
    }
}
