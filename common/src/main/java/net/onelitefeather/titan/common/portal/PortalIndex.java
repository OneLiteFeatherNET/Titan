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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Answers "which portal is this position in" without walking the portal list.
 *
 * <p>The reason this class exists is the call site: {@code PlayerMoveEvent} fires for every
 * movement packet of every player, several times a second each. Testing every portal's bounds on
 * every one of those is work proportional to {@code players x portals} for an answer that is
 * almost always "none".
 *
 * <p>So portals are bucketed by <b>chunk column</b> once, at load time. A lookup takes the chunk
 * coordinates already carried by the position, packs them into one {@code long} key, and does a
 * single {@link HashMap} lookup; only the portals sharing that column - normally none, and at
 * worst the handful an operator deliberately put in the same 16x16 area - are asked whether they
 * contain the point. Cost per movement is therefore one hash lookup and, in the common case, no
 * geometry at all. Y is left to the shape: portals overlap in columns far more rarely than they
 * overlap in height, and a third dimension in the key would only spread the same few portals over
 * more buckets.
 *
 * <p>The index is immutable. Reloading the configuration builds a new one rather than mutating
 * this one, which is what keeps the lookup free of locks on the movement path.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class PortalIndex {

    private static final PortalIndex EMPTY = new PortalIndex(Map.of(), List.of());

    private final Map<Long, List<Portal>> byChunkColumn;
    private final List<Portal> portals;

    private PortalIndex(Map<Long, List<Portal>> byChunkColumn, List<Portal> portals) {
        this.byChunkColumn = byChunkColumn;
        this.portals = portals;
    }

    /**
     * Returns the index without any portal, whose lookup is a constant {@code null}.
     *
     * @return the empty index
     */
    @Contract(pure = true)
    public static PortalIndex empty() {
        return EMPTY;
    }

    /**
     * Builds an index over the given portals. Every portal is registered in each chunk column its
     * region touches; a portal spanning several columns is therefore found from any of them.
     *
     * @param portals the portals to index
     * @return an index over those portals
     */
    public static PortalIndex of(Collection<Portal> portals) {
        if (portals.isEmpty()) {
            return EMPTY;
        }
        Map<Long, List<Portal>> buckets = new HashMap<>();
        for (Portal portal : portals) {
            Point from = portal.region().min();
            Point to = portal.region().max();
            for (int chunkX = from.chunkX(); chunkX <= to.chunkX(); chunkX++) {
                for (int chunkZ = from.chunkZ(); chunkZ <= to.chunkZ(); chunkZ++) {
                    buckets.computeIfAbsent(key(chunkX, chunkZ), ignored -> new ArrayList<>()).add(portal);
                }
            }
        }
        Map<Long, List<Portal>> frozen = new HashMap<>(buckets.size());
        buckets.forEach((column, bucket) -> frozen.put(column, List.copyOf(bucket)));
        return new PortalIndex(Map.copyOf(frozen), List.copyOf(portals));
    }

    /**
     * Returns the portal containing the given position.
     *
     * @param position the position to look up, usually a player's new position
     * @return the portal the position is inside, or {@code null} when it is inside none. When
     *         regions overlap, the first portal registered for the column wins - overlapping
     *         portals are a configuration mistake, not a supported layout.
     */
    @Contract(pure = true)
    public @Nullable Portal portalAt(Point position) {
        if (this.byChunkColumn.isEmpty()) {
            return null;
        }
        List<Portal> candidates = this.byChunkColumn.get(key(position.chunkX(), position.chunkZ()));
        if (candidates == null) {
            return null;
        }
        for (Portal portal : candidates) {
            if (portal.contains(position)) {
                return portal;
            }
        }
        return null;
    }

    /**
     * Returns every indexed portal, in configuration order.
     *
     * @return the indexed portals
     */
    @Contract(pure = true)
    public @Unmodifiable List<Portal> portals() {
        return this.portals;
    }

    /**
     * Returns whether this index holds no portal at all.
     *
     * @return whether the index is empty
     */
    @Contract(pure = true)
    public boolean isEmpty() {
        return this.portals.isEmpty();
    }

    /**
     * Returns how many chunk columns hold at least one portal. Exposed for tests, which assert
     * that the index really is sparse rather than one bucket holding everything.
     *
     * @return the number of occupied chunk columns
     */
    @Contract(pure = true)
    public int occupiedColumns() {
        return this.byChunkColumn.size();
    }

    private static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
