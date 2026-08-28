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
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The pinned season: the preview path, and the reason the boundaries are a strategy at all.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
class FixedSeasonStrategyTest {

    @ParameterizedTest(name = "pinned to {0}")
    @EnumSource(Season.class)
    @DisplayName("The pinned season is returned whatever the date")
    void thePinnedSeasonIgnoresTheDate(Season season) {
        SeasonBoundaryStrategy strategy = SeasonBoundaryStrategy.fixed(season);

        assertEquals(season, strategy.seasonAt(LocalDate.of(2026, 1, 1)));
        assertEquals(season, strategy.seasonAt(LocalDate.of(2026, 8, 15)));
        assertEquals(season, strategy.seasonAt(LocalDate.of(2026, 12, 31)));
        assertEquals(season, strategy.seasonAt(LocalDate.of(1970, 1, 1)));
    }

    @Test
    @DisplayName("A winter event can be shown in August without touching the system clock")
    void aWinterEventCanBeShownInAugust() {
        SeasonBoundaryStrategy preview = SeasonBoundaryStrategy.fixed(Season.WINTER);

        assertEquals(Season.SUMMER, SeasonBoundaryStrategy.meteorological().seasonAt(LocalDate.of(2026, 8, 15)));
        assertEquals(Season.WINTER, preview.seasonAt(LocalDate.of(2026, 8, 15)));
    }

    @Test
    @DisplayName("Two strategies pinned to the same season are equal")
    void pinnedStrategiesAreValues() {
        assertEquals(FixedSeasonStrategy.of(Season.WINTER), FixedSeasonStrategy.of(Season.WINTER));
        assertEquals(Season.WINTER, FixedSeasonStrategy.of(Season.WINTER).season());
    }
}
