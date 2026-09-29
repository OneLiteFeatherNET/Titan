/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.feature.portal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.CoordConversion;
import net.onelitefeather.titan.core.portal.HorizontalBounds;
import net.onelitefeather.titan.core.portal.Portal;

/**
 * Portals by chunk column, so a move only tests the few portals near it instead of all of them.
 * Built once; read-only afterwards, so the move listener needs no locking.
 */
final class PortalIndex {

    private static final List<Portal> NONE = List.of();

    private final Map<Long, List<Portal>> byColumn = new HashMap<>();

    PortalIndex(List<Portal> portals) {
        for (Portal portal : portals) {
            HorizontalBounds bounds = portal.shape().horizontalBounds();
            int minChunkX = CoordConversion.globalToChunk(bounds.minX());
            int maxChunkX = CoordConversion.globalToChunk(bounds.maxX());
            int minChunkZ = CoordConversion.globalToChunk(bounds.minZ());
            int maxChunkZ = CoordConversion.globalToChunk(bounds.maxZ());
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    this.byColumn.computeIfAbsent(CoordConversion.chunkIndex(chunkX, chunkZ), key -> new ArrayList<>()).add(portal);
                }
            }
        }
    }

    /**
     * The portals in the chunk columns of both ends of a move. Both, because a step can end in a
     * column that does not hold the portal it started next to, and the other way round.
     */
    List<Portal> candidates(Point from, Point to) {
        List<Portal> atFrom = column(from);
        List<Portal> atTo = column(to);
        if (atFrom.isEmpty() && atTo.isEmpty()) {
            return NONE;
        }
        Set<Portal> merged = new LinkedHashSet<>(atFrom);
        merged.addAll(atTo);
        return List.copyOf(merged);
    }

    private List<Portal> column(Point point) {
        return this.byColumn.getOrDefault(CoordConversion.chunkIndex(CoordConversion.globalToChunk(point.x()), CoordConversion.globalToChunk(point.z())), NONE);
    }
}
