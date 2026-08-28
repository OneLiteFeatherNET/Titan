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
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.coris.shape.CuboidShape;
import net.onelitefeather.deliver.DeliverType;
import net.onelitefeather.titan.common.feature.TitanFeatures;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Optional;

/**
 * One portal exactly as it stands in {@code portals.json}. Every field is nullable and every
 * field is a string or a plain vector, because this is what a hand-written file deserialises to -
 * validation happens in {@link #resolve()}, not in the constructor, so one bad entry costs its own
 * portal and not the whole file (US-7.02).
 *
 * <p>An entry that cannot be resolved is dropped with a log line naming it. Dropping is the safe
 * direction: a portal that does not exist sends nobody anywhere, while a portal built from a
 * half-understood entry would.
 *
 * @param id      operator-facing id, unique within the file
 * @param type    {@code task} (default) or {@code server}
 * @param target  the CloudNet task or service to deliver to
 * @param feature name of the {@link TitanFeatures} constant guarding the same destination in the
 *                navigator, or {@code null} for a destination open to everyone
 * @param min     one corner of the region
 * @param max     the opposite corner; corners are normalised and the region is block-inclusive
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public record PortalDefinition(@Nullable String id, @Nullable String type, @Nullable String target,
                               @Nullable String feature, @Nullable Vec min, @Nullable Vec max) {

    /**
     * Upper bound on the chunk columns a single portal may span, guarding the index against a
     * mistyped coordinate: {@value} columns is a 1024x1024 block footprint, far beyond any lobby
     * portal, and a region larger than that is a typo rather than a portal.
     */
    static final int MAX_CHUNK_COLUMNS = 4096;

    private static final Logger LOGGER = LoggerFactory.getLogger(PortalDefinition.class);

    /**
     * Validates this entry and turns it into a usable {@link Portal}.
     *
     * @return the portal, or an empty optional when the entry is unusable - in which case the
     *         reason has been logged
     */
    public Optional<Portal> resolve() {
        String portalId = this.id == null ? "" : this.id.trim();
        if (portalId.isEmpty()) {
            LOGGER.warn("Ignoring a portal without an id");
            return Optional.empty();
        }
        if (this.target == null || this.target.isBlank()) {
            LOGGER.warn("Ignoring portal '{}': it names no target server", portalId);
            return Optional.empty();
        }
        if (this.min == null || this.max == null) {
            LOGGER.warn("Ignoring portal '{}': its region needs both a min and a max corner", portalId);
            return Optional.empty();
        }
        Optional<DeliverType> deliverType = deliverType();
        if (deliverType.isEmpty()) {
            LOGGER.warn("Ignoring portal '{}': unknown target type '{}', expected 'task' or 'server'", portalId, this.type);
            return Optional.empty();
        }
        TitanFeatures gate;
        if (this.feature == null || this.feature.isBlank()) {
            gate = null;
        } else {
            Optional<TitanFeatures> resolved = feature(this.feature);
            if (resolved.isEmpty()) {
                LOGGER.warn("Ignoring portal '{}': unknown feature '{}'. A portal that names a feature nobody can resolve would be ungated, which is the opposite of what was configured", portalId, this.feature);
                return Optional.empty();
            }
            gate = resolved.get();
        }
        CuboidShape region;
        try {
            region = new CuboidShape(this.min, this.max);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Ignoring portal '{}': its two corners are identical, so the region has no volume. Coris needs two distinct corners; use {} and {} for a single block", portalId, this.min, this.max.add(1, 1, 1));
            return Optional.empty();
        }
        if (chunkColumns(region) > MAX_CHUNK_COLUMNS) {
            LOGGER.warn("Ignoring portal '{}': its region spans more than {} chunk columns, which is a mistyped coordinate rather than a portal", portalId, MAX_CHUNK_COLUMNS);
            return Optional.empty();
        }
        return Optional.of(new Portal(portalId, region, deliverType.get(), this.target.trim(), gate));
    }

    private Optional<DeliverType> deliverType() {
        if (this.type == null || this.type.isBlank()) {
            return Optional.of(DeliverType.TASK);
        }
        String normalized = this.type.trim().toUpperCase(Locale.ROOT);
        for (DeliverType candidate : DeliverType.values()) {
            if (candidate.name().equals(normalized)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static Optional<TitanFeatures> feature(String name) {
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        for (TitanFeatures candidate : TitanFeatures.values()) {
            if (candidate.name().equals(normalized)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /**
     * Counts the chunk columns a region touches - the number of buckets it would occupy in
     * {@link PortalIndex}.
     */
    private static long chunkColumns(CuboidShape region) {
        Point from = region.min();
        Point to = region.max();
        long columnsX = (long) to.chunkX() - from.chunkX() + 1;
        long columnsZ = (long) to.chunkZ() - from.chunkZ() + 1;
        return columnsX * columnsZ;
    }
}
