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

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * The default {@link SeasonService}; reached through {@link SeasonService#create(Clock)}.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
final class TitanSeasonService implements SeasonService {

    private final Clock clock;
    private final ZoneId zone;
    private final SeasonBoundaryStrategy strategy;

    TitanSeasonService(Clock clock, ZoneId zone, SeasonBoundaryStrategy strategy) {
        this.clock = clock;
        this.zone = zone;
        this.strategy = strategy;
    }

    @Override
    public Season currentSeason() {
        return this.strategy.seasonAt(currentDate());
    }

    @Override
    public LocalDate currentDate() {
        return this.clock.instant().atZone(this.zone).toLocalDate();
    }

    @Override
    public ZoneId zone() {
        return this.zone;
    }

    @Override
    public SeasonBoundaryStrategy strategy() {
        return this.strategy;
    }

    @Override
    public String toString() {
        return "TitanSeasonService[zone=" + this.zone + ", strategy=" + this.strategy + ']';
    }
}
