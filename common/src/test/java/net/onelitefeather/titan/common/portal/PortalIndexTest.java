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

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.coris.shape.CuboidShape;
import net.onelitefeather.deliver.DeliverType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalIndexTest {

    /** Blocks 0..4 on every axis, so the region sits inside chunk column (0, 0). */
    private static final Portal SPAWN_PORTAL = portal("spawn", new Vec(0, 64, 0), new Vec(4, 68, 4));

    private static Portal portal(String id, Vec min, Vec max) {
        return new Portal(id, new CuboidShape(min, max), DeliverType.TASK, "Survival", null);
    }

    @Test
    @DisplayName("a position in the middle of a portal finds it")
    void findsPortalInside() {
        PortalIndex index = PortalIndex.of(List.of(SPAWN_PORTAL));

        assertSame(SPAWN_PORTAL, index.portalAt(new Pos(2.5, 66, 2.5)));
    }

    @ParameterizedTest(name = "[{index}] ({0}, {1}, {2}) is inside: {3}")
    @DisplayName("the region is block-inclusive on both corners")
    @CsvSource({
            // The minimum corner itself belongs to the portal.
            "0.0, 64.0, 0.0, true",
            // Anywhere within block 4 still belongs to it - the max corner is inclusive.
            "4.999, 68.999, 4.999, true",
            // The first block past the max corner does not.
            "5.0, 66.0, 2.5, false", "2.5, 69.0, 2.5, false", "2.5, 66.0, 5.0, false",
            // Nor does the block before the min corner; -0.001 is block -1, not block 0.
            "-0.001, 66.0, 2.5, false", "2.5, 63.999, 2.5, false", "2.5, 66.0, -0.001, false"})
    void respectsRegionEdges(double x, double y, double z, boolean inside) {
        PortalIndex index = PortalIndex.of(List.of(SPAWN_PORTAL));

        Portal found = index.portalAt(new Pos(x, y, z));

        assertEquals(inside, found != null, "position (" + x + ", " + y + ", " + z + ")");
    }

    @Test
    @DisplayName("a position in the same chunk but outside the region finds nothing")
    void missesInsideSameChunk() {
        PortalIndex index = PortalIndex.of(List.of(SPAWN_PORTAL));

        // Block 10 is still chunk column (0, 0): the bucket is hit, the geometry is not.
        assertNull(index.portalAt(new Pos(10.5, 66, 10.5)));
    }

    @Test
    @DisplayName("a position in a chunk without portals finds nothing")
    void missesInOtherChunk() {
        PortalIndex index = PortalIndex.of(List.of(SPAWN_PORTAL));

        assertNull(index.portalAt(new Pos(500.5, 66, 500.5)));
    }

    @Test
    @DisplayName("a portal crossing a chunk border is found from either side")
    void findsPortalAcrossChunkBorder() {
        Portal wide = portal("wide", new Vec(12, 64, 12), new Vec(20, 68, 20));
        PortalIndex index = PortalIndex.of(List.of(wide));

        assertSame(wide, index.portalAt(new Pos(13.5, 66, 13.5)), "chunk (0, 0) side");
        assertSame(wide, index.portalAt(new Pos(18.5, 66, 18.5)), "chunk (1, 1) side");
        assertEquals(4, index.occupiedColumns(), "the portal spans a 2x2 block of chunk columns");
    }

    @Test
    @DisplayName("negative coordinates do not collide with positive ones in the column key")
    void separatesNegativeAndPositiveColumns() {
        Portal negative = portal("negative", new Vec(-40, 64, -40), new Vec(-36, 68, -36));
        PortalIndex index = PortalIndex.of(List.of(SPAWN_PORTAL, negative));

        assertSame(negative, index.portalAt(new Pos(-38.5, 66, -38.5)));
        assertSame(SPAWN_PORTAL, index.portalAt(new Pos(2.5, 66, 2.5)));
        assertNull(index.portalAt(new Pos(-38.5, 66, 2.5)), "mirrored coordinates are a different column");
    }

    @Test
    @DisplayName("portals are spread over columns instead of piling into one bucket")
    void bucketsStaySparse() {
        List<Portal> portals = List.of(SPAWN_PORTAL, portal("far", new Vec(1000, 64, 1000), new Vec(1004, 68, 1004)), portal("further", new Vec(-2000, 64, -2000), new Vec(-1996, 68, -1996)));

        PortalIndex index = PortalIndex.of(portals);

        assertEquals(3, index.occupiedColumns());
        assertEquals(3, index.portals().size());
        assertNotNull(index.portalAt(new Pos(1002.5, 66, 1002.5)));
    }

    @Test
    @DisplayName("the empty index answers without touching anything")
    void emptyIndexFindsNothing() {
        assertTrue(PortalIndex.empty().isEmpty());
        assertNull(PortalIndex.empty().portalAt(new Pos(0, 0, 0)));
        assertEquals(0, PortalIndex.empty().occupiedColumns());
        assertTrue(PortalIndex.of(List.of()).isEmpty());
    }
}
