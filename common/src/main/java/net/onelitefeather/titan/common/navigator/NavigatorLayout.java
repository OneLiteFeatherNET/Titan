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
package net.onelitefeather.titan.common.navigator;

import net.onelitefeather.titan.common.feature.FeatureAudience;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Turns the entries a player may see into slot positions.
 *
 * <p>This is the answer to US-5.02 and NFR-005. A fixed layout cannot satisfy them: if the build
 * servers owned slots of their own, a player without
 * {@value BuildServerAccess#PERMISSION} would find those slots empty every time, and an empty
 * slot that is only ever empty for some players is itself the information the requirement wants
 * hidden. So no slot belongs to an entry. The visible entries are laid out as one uninterrupted,
 * centred block, and the block is computed from the visible entries alone — a filtered-out entry
 * never existed as far as the layout is concerned.
 *
 * <p>The consequence is the property the tests assert: the menu a player without the permission
 * sees is byte-for-byte the menu of a lobby that has no build servers at all. There is nothing to
 * count, nothing to compare against and no hole to notice.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class NavigatorLayout {

    private NavigatorLayout() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Places the entries the player may see into a row of the given size.
     *
     * <p>Entries keep the order they were handed in. If more entries are visible than fit, the
     * surplus is dropped from the end rather than spilling into a second row, so the menu keeps
     * the size every player sees.
     *
     * @param entries  every entry the navigator could offer, in display order
     * @param playerId the player the menu is drawn for
     * @param audience the source of permission answers
     * @param size     the number of slots available
     * @return the visible entries with their slots, in ascending slot order
     */
    @Contract(pure = true)
    public static List<Placement> plan(List<NavigatorEntry> entries, UUID playerId, FeatureAudience audience, int size) {
        List<NavigatorEntry> visible = visibleTo(entries, playerId, audience);
        int shown = Math.min(visible.size(), size);
        int start = (size - shown) / 2;
        List<Placement> placements = new ArrayList<>(shown);
        for (int index = 0; index < shown; index++) {
            placements.add(new Placement(start + index, visible.get(index)));
        }
        return List.copyOf(placements);
    }

    /**
     * Filters the entries down to the ones the player is allowed to be offered.
     *
     * @param entries  every entry the navigator could offer, in display order
     * @param playerId the player the menu is drawn for
     * @param audience the source of permission answers
     * @return the visible entries, order preserved
     */
    @Contract(pure = true)
    public static List<NavigatorEntry> visibleTo(List<NavigatorEntry> entries, UUID playerId, FeatureAudience audience) {
        List<NavigatorEntry> visible = new ArrayList<>(entries.size());
        for (NavigatorEntry entry : entries) {
            if (entry.isVisibleTo(playerId, audience)) {
                visible.add(entry);
            }
        }
        return List.copyOf(visible);
    }

    /**
     * One entry and the slot it was given.
     *
     * @param slot  the slot index inside the menu
     * @param entry the entry shown there
     * @author TheMeinerLP
     * @version 1.0.0
     * @since 1.15.0
     */
    public record Placement(int slot, NavigatorEntry entry) {
    }
}
