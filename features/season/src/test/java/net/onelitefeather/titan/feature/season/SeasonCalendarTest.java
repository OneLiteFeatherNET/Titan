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
package net.onelitefeather.titan.feature.season;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SeasonCalendarTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private final SeasonCalendar calendar = new SeasonCalendar();

    private static Season season(String id, String from, String to) {
        return new Season(id, id, LocalDateTime.parse(from), LocalDateTime.parse(to), true);
    }

    private static Season winter() {
        return season("winter", "2026-12-01T00:00:00", "2027-01-07T00:00:00");
    }

    private Optional<String> activeId(List<Season> seasons, String utcInstant, ZoneId zone) {
        return this.calendar.activeAt(seasons, Instant.parse(utcInstant), zone).map(Season::id);
    }

    @DisplayName("A season is active inside its window")
    @Test
    void seasonIsActiveInsideItsWindow() {
        Assertions.assertEquals(Optional.of("winter"), activeId(List.of(winter()), "2026-12-24T12:00:00Z", BERLIN));
    }

    @DisplayName("The window includes its start")
    @Test
    void windowIncludesItsStart() {
        // 00:00 Berlin (CET) is 23:00 UTC the day before.
        Assertions.assertEquals(Optional.of("winter"), activeId(List.of(winter()), "2026-11-30T23:00:00Z", BERLIN));
    }

    @DisplayName("The window excludes its end")
    @Test
    void windowExcludesItsEnd() {
        Assertions.assertEquals(Optional.empty(), activeId(List.of(winter()), "2027-01-06T23:00:00Z", BERLIN));
    }

    @DisplayName("Before the window no season is active")
    @Test
    void noSeasonBeforeTheWindow() {
        Assertions.assertEquals(Optional.empty(), activeId(List.of(winter()), "2026-11-30T22:59:59Z", BERLIN));
    }

    @DisplayName("Without seasons nothing is active")
    @Test
    void noSeasonsMeansNoActiveSeason() {
        Assertions.assertEquals(Optional.empty(), activeId(List.of(), "2026-12-24T12:00:00Z", BERLIN));
    }

    @DisplayName("A disabled season is ignored")
    @Test
    void disabledSeasonIsIgnored() {
        Season disabled = new Season("winter", "winter", winter().from(), winter().to(), false);

        Assertions.assertEquals(Optional.empty(), activeId(List.of(disabled), "2026-12-24T12:00:00Z", BERLIN));
    }

    @DisplayName("The zone shifts the window")
    @Test
    void zoneShiftsTheWindow() {
        List<Season> seasons = List.of(winter());
        // 2026-11-30T16:00Z is 01:00 in Tokyo on Dec 1, but still 17:00 on Nov 30 in Berlin.
        String instant = "2026-11-30T16:00:00Z";

        Assertions.assertEquals(Optional.of("winter"), activeId(seasons, instant, ZoneId.of("Asia/Tokyo")), "Tokyo has reached from");
        Assertions.assertEquals(Optional.empty(), activeId(seasons, instant, BERLIN), "Berlin has not");
    }

    @DisplayName("On overlap the earlier start wins")
    @Test
    void earlierStartWinsOnOverlap() {
        Season autumn = season("autumn", "2026-10-01T00:00:00", "2026-11-15T00:00:00");
        Season halloween = season("halloween", "2026-10-25T00:00:00", "2026-11-02T00:00:00");

        Assertions.assertEquals(Optional.of("autumn"), activeId(List.of(halloween, autumn), "2026-10-28T12:00:00Z", BERLIN));
    }

    @DisplayName("On overlap with the same start the smaller id wins")
    @Test
    void smallerIdWinsOnSameStart() {
        Season b = season("b", "2026-10-01T00:00:00", "2026-11-15T00:00:00");
        Season a = season("a", "2026-10-01T00:00:00", "2026-11-02T00:00:00");

        Assertions.assertEquals(Optional.of("a"), activeId(List.of(b, a), "2026-10-28T12:00:00Z", BERLIN));
    }

    @DisplayName("Overlapping enabled seasons are reported with the winner first")
    @Test
    void overlapsNameWinnerAndLoser() {
        Season autumn = season("autumn", "2026-10-01T00:00:00", "2026-11-15T00:00:00");
        Season halloween = season("halloween", "2026-10-25T00:00:00", "2026-11-02T00:00:00");

        List<SeasonCalendar.Overlap> overlaps = this.calendar.overlaps(List.of(halloween, autumn));

        Assertions.assertEquals(List.of(new SeasonCalendar.Overlap(autumn, halloween)), overlaps);
    }

    @DisplayName("Windows that only touch do not overlap")
    @Test
    void touchingWindowsDoNotOverlap() {
        Season first = season("first", "2026-10-01T00:00:00", "2026-11-01T00:00:00");
        Season second = season("second", "2026-11-01T00:00:00", "2026-12-01T00:00:00");

        Assertions.assertEquals(List.of(), this.calendar.overlaps(List.of(first, second)));
    }

    @DisplayName("A disabled season takes part in no overlap")
    @Test
    void disabledSeasonHasNoOverlap() {
        Season disabled = new Season("autumn", "autumn", LocalDateTime.parse("2026-10-01T00:00:00"), LocalDateTime.parse("2026-11-15T00:00:00"), false);
        Season halloween = season("halloween", "2026-10-25T00:00:00", "2026-11-02T00:00:00");

        Assertions.assertEquals(List.of(), this.calendar.overlaps(List.of(disabled, halloween)));
    }
}
