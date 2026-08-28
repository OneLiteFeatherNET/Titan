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

import org.jetbrains.annotations.Contract;

/**
 * Seasonal content that can be switched on and, above all, switched off again (US-4.01, US-4.02).
 *
 * <p>{@link #deactivate()} is the method this interface exists for. Placing decoration is easy and
 * gets tested by anybody who looks at the lobby; removing it happens once, months later, usually
 * on the day a new season is being deployed, and nobody is watching. So the contract is
 * deliberately
 * blunt: content that cannot take itself back out of the world is not finished content, and the
 * test for it asserts what the world looks like before and after rather than that the method ran.
 *
 * <p>Both methods are idempotent. Activating twice is one activation, deactivating twice is one
 * deactivation, and deactivating something that was never activated does nothing — the lobby
 * re-evaluates seasons on a timer, so both calls will happen more than once.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public interface SeasonalContent {

    /**
     * Returns the id of this content, as written in its file.
     *
     * @return the season id
     */
    @Contract(pure = true)
    String id();

    /**
     * Returns which season wins where two overlap: higher applies later, and therefore on top
     * (US-4.05).
     *
     * @return the priority from the season file
     */
    @Contract(pure = true)
    int priority();

    /**
     * Returns whether this content is currently applied to the world.
     *
     * @return whether the content is active
     */
    @Contract(pure = true)
    boolean active();

    /**
     * Applies the content to the world, recording enough to undo every change.
     *
     * @param canvas the world to apply the content to
     */
    void activate(SeasonCanvas canvas);

    /**
     * Takes every change back, in the reverse order it was made.
     *
     * <p>Reverse order is not tidiness. Two seasons may have written to the same position, and only
     * undoing in reverse puts back what the other one had put there rather than what was underneath
     * both.
     */
    void deactivate();
}
