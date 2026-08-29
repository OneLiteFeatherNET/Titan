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

import org.jetbrains.annotations.Nullable;

/**
 * The JSON shape of {@value TimeConfig#TIME_FILE_NAME}.
 *
 * <p>The three values stay {@link String}s rather than enums on purpose. Gson maps an unknown enum
 * constant to {@code null}, which is indistinguishable from an absent key — and an absent key is
 * what keeps the default. Reading the raw text and resolving it in
 * {@link TimeConfig#resolveDayTimeStrategy()} and {@link TimeConfig#resolveSeasonStrategy} is what
 * lets a typo be reported as a typo.
 *
 * @param dayTimeStrategy the day time strategy name as written in the file
 * @param seasonStrategy  the season strategy name as written in the file
 * @param fixedSeason     the pinned season name as written in the file
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
record TimeConfigImpl(@Nullable String dayTimeStrategy, @Nullable String seasonStrategy,
                      @Nullable String fixedSeason) implements TimeConfig {

    /** The stage 2 defaults, written out when no file exists yet (US-2.06, US-2.11). */
    static final TimeConfigImpl DEFAULT = new TimeConfigImpl(TimeConfig.LINEAR, TimeConfig.METEOROLOGICAL, null);
}
