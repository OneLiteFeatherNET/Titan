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
package net.onelitefeather.titan.common.season;

import java.time.ZoneId;
import java.util.Optional;

/**
 * Turns the name of a recurring season into the dates it covers in one year.
 *
 * <p>This is the seam left for spec stage 2. A season file normally spells its window out:
 *
 * <pre>{@code "window": { "from": "2026-12-01", "to": "2026-12-27" }}</pre>
 *
 * <p>which is right for an event with a chosen start date, and wrong for anything tied to the
 * calendar rather than to the marketing plan. Winter does not begin on a date somebody typed; it
 * begins where the boundary strategy says it does, and it moves by a day or so every year. Written
 * out, such a window has to be corrected annually, and the year it is not corrected is the year
 * nobody notices.
 *
 * <p>So a file may name its window instead:
 *
 * <pre>{@code "window": { "named": "WINTER", "year": 2026 }}</pre>
 *
 * <p>and {@link SeasonLoader} asks this resolver what those dates are. Stage 2 implements it over
 * its {@code SeasonBoundaryStrategy} and {@code Season} types and passes the implementation to
 * {@link SeasonLoader#create(ZoneId, NamedWindowResolver)}; nothing in this package needs to know
 * those types exist, and nothing here has to change when they arrive.
 *
 * <p>Until then {@link #unavailable()} is installed, and a file that names a window fails to load
 * with a message saying so rather than quietly running all year.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
@FunctionalInterface
public interface NamedWindowResolver {

    /**
     * Returns the resolver used while no season strategies are installed. It resolves nothing, so
     * every named window is reported as unknown by name.
     *
     * @return a resolver that never resolves anything
     */
    static NamedWindowResolver unavailable() {
        return (name, year, zone) -> Optional.empty();
    }

    /**
     * Resolves a named window to the dates it covers.
     *
     * @param name the name from the season file, for example {@code WINTER}
     * @param year the year the season is being planned for
     * @param zone the zone the resulting local times are read in
     * @return the window, or an empty optional when this resolver does not know the name
     */
    Optional<SeasonWindow> resolve(String name, int year, ZoneId zone);
}
