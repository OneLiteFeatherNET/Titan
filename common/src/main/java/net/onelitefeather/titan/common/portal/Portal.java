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
package net.onelitefeather.titan.common.portal;

import net.minestom.server.coordinate.Point;
import net.onelitefeather.coris.shape.Shape;
import net.onelitefeather.deliver.DeliverType;
import net.onelitefeather.titan.common.utils.TitanFeatures;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * A validated portal: a region of the lobby, the server it hands players to, and the navigator
 * feature that decides who may use it.
 *
 * <p>This is the shape a portal has after {@link PortalDefinition#resolve()} accepted it. A
 * definition an operator mistyped never becomes a {@link Portal}, so nothing downstream has to
 * re-check the configuration.
 *
 * <p>The region is a Coris {@link Shape}: containment, normalisation of the two corners and the
 * block-inclusive bounds all come from the org's shape library rather than from a bounding box
 * written here (OLF-L2-04).
 *
 * @param id      the operator-facing id, used in logs and to recognise re-entry
 * @param region  the area a player has to be standing in
 * @param type    whether {@link #target()} names a CloudNet task or a single service
 * @param target  the task or service a player is delivered to
 * @param feature the navigator feature guarding the same destination, or {@code null} when the
 *                destination is open to everyone
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public record Portal(String id, Shape region, DeliverType type, String target,
                     @Nullable TitanFeatures feature) {

    /**
     * Checks whether the given position lies inside this portal.
     *
     * @param position the position to test
     * @return whether the position is inside the region
     */
    @Contract(pure = true)
    public boolean contains(Point position) {
        return this.region.intersect(position);
    }

    /**
     * Returns whether using this portal is guarded by a feature.
     *
     * @return whether a feature has to admit the player first
     */
    @Contract(pure = true)
    public boolean isGated() {
        return this.feature != null;
    }
}
