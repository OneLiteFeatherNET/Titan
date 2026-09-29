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

import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Portal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PortalIndexTest {

    private static Portal box(String id, Vec min, Vec max) {
        return new Portal(id, new Box(min, max), "Survival", null);
    }

    @DisplayName("A portal inside one chunk column is found from a position in that column")
    @Test
    void findsPortalFromItsOwnColumn() {
        Portal portal = box("a", new Vec(2, 64, 2), new Vec(3, 65, 3));
        PortalIndex index = new PortalIndex(List.of(portal));

        List<Portal> found = index.candidates(new Pos(8, 64, 8), new Pos(9, 64, 8));

        Assertions.assertEquals(List.of(portal), found, "both endpoints lie in the portal's column");
    }

    @DisplayName("A portal over a chunk border is indexed in both columns")
    @Test
    void portalOverChunkBorderIsFoundFromBothColumns() {
        Portal portal = box("border", new Vec(15, 64, 2), new Vec(16, 65, 3));
        PortalIndex index = new PortalIndex(List.of(portal));

        Assertions.assertEquals(List.of(portal), index.candidates(new Pos(5, 64, 5), new Pos(5, 64, 5)), "the west column must know the portal");
        Assertions.assertEquals(List.of(portal), index.candidates(new Pos(20, 64, 5), new Pos(20, 64, 5)), "the east column must know the portal");
    }

    @DisplayName("A move over the chunk border finds the portal through the other endpoint's column")
    @Test
    void movingAcrossBorderFindsPortalThroughOtherEndpoint() {
        Portal portal = box("east", new Vec(20, 64, 2), new Vec(21, 65, 3));
        PortalIndex index = new PortalIndex(List.of(portal));

        List<Portal> found = index.candidates(new Pos(10, 64, 2), new Pos(20.5, 64, 2));

        Assertions.assertEquals(List.of(portal), found, "the destination column holds the portal");
    }

    @DisplayName("A world without portals yields an empty list")
    @Test
    void worldWithoutPortalsYieldsEmptyList() {
        PortalIndex index = new PortalIndex(List.of());

        Assertions.assertTrue(index.candidates(new Pos(0, 64, 0), new Pos(1, 64, 1)).isEmpty(), "no portals, no candidates");
    }

    @DisplayName("A position in a column without portals yields nothing")
    @Test
    void columnWithoutPortalsYieldsNothing() {
        PortalIndex index = new PortalIndex(List.of(box("a", new Vec(2, 64, 2), new Vec(3, 65, 3))));

        Assertions.assertTrue(index.candidates(new Pos(100, 64, 100), new Pos(101, 64, 100)).isEmpty(), "a far column knows no portal");
    }

    @DisplayName("A portal found from both endpoints and from several columns is returned once")
    @Test
    void portalIsNeverReturnedTwice() {
        Portal portal = box("wide", new Vec(10, 64, 10), new Vec(40, 65, 12));
        PortalIndex index = new PortalIndex(List.of(portal));

        List<Portal> found = index.candidates(new Pos(12, 64, 10), new Pos(36, 64, 10));

        Assertions.assertEquals(List.of(portal), found, "dedup across endpoints and columns");
    }
}
