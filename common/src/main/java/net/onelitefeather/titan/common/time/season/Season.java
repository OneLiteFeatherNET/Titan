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

import org.jetbrains.annotations.Contract;

import java.util.Locale;

/**
 * The four seasons of the year, in calendar order starting with spring.
 *
 * <p>The enum is the state the rest of the lobby reads (US-2.09). Which dates it changes on is not
 * its business — that is the job of a {@link SeasonBoundaryStrategy}, and the point of keeping the
 * two apart is that a seasonal package can be previewed in August without touching the system
 * clock.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public enum Season {

    /** March to May under meteorological boundaries. */
    SPRING,

    /** June to August under meteorological boundaries. */
    SUMMER,

    /** September to November under meteorological boundaries. */
    AUTUMN,

    /** December to February under meteorological boundaries. */
    WINTER;

    /**
     * Returns the lower-case identifier used in configuration files and world directory names.
     *
     * @return the identifier, for example {@code "winter"}
     */
    @Contract(pure = true)
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the season that follows this one.
     *
     * @return the next season in calendar order, wrapping from winter to spring
     */
    @Contract(pure = true)
    public Season next() {
        Season[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
