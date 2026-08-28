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

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;

/**
 * Everything a season is allowed to change about the running lobby.
 *
 * <p>The interface is small, and every method on it has an inverse. That is the whole reason it
 * exists: a season that can only reach the world through operations it can also undo cannot leave
 * anything behind, and {@link SeasonalContent#deactivate()} becomes a property of the design
 * rather than a promise somebody has to keep (US-4.02).
 *
 * <p>{@link #blockAt(Point)} is on here for the same reason. A season reads what is at a position
 * before it writes to it, so the undo is "put back what was actually there" rather than "put back
 * air", which is the version that leaves a hole in the roof of a build.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public interface SeasonCanvas {

    /**
     * Reads the block at a position.
     *
     * @param position the position to read
     * @return the block currently there
     */
    Block blockAt(Point position);

    /**
     * Writes a block to a position.
     *
     * @param position the position to write to
     * @param block    the block to put there
     */
    void setBlock(Point position, Block block);

    /**
     * Spawns a floating text display.
     *
     * @param position where the display floats
     * @param text     the text it shows
     * @return the id the display can be removed by
     */
    UUID spawnDisplay(Pos position, Component text);

    /**
     * Removes a display spawned by {@link #spawnDisplay(Pos, Component)}. Removing a display that
     * is already gone does nothing.
     *
     * @param displayId the id returned when the display was spawned
     */
    void removeDisplay(UUID displayId);

    /**
     * Plays a sound at a position, for everybody who can hear it.
     *
     * @param position where the sound comes from
     * @param sound    the sound to play
     */
    void playSound(Pos position, Key sound);

    /**
     * Returns the message prefix in force right now.
     *
     * @return the prefix every {@code <prefix>} tag currently resolves to
     */
    Component prefix();

    /**
     * Sets the message prefix.
     *
     * @param prefix the prefix to use, or {@code null} to go back to the lobby's own
     */
    void prefix(@Nullable Component prefix);

    /**
     * Schedules a repeating action.
     *
     * @param period how long to wait between two runs, and before the first
     * @param action what to run
     * @return the handle the season keeps so it can stop the action again
     */
    Handle schedule(Duration period, Runnable action);

    /**
     * A scheduled action a season can stop.
     *
     * @author TheMeinerLP
     * @version 1.0.0
     * @since 1.15.0
     */
    interface Handle {

        /** Stops the action. Cancelling twice is allowed and does nothing the second time. */
        void cancel();

        /**
         * Returns whether the action is still scheduled.
         *
         * @return whether the action would still run
         */
        boolean alive();
    }
}
