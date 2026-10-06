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
package net.onelitefeather.titan.setup.portal.preview;

import net.onelitefeather.titan.setup.portal.editor.DraftOutline;
import net.onelitefeather.titan.setup.portal.editor.PortalDraft;
import net.onelitefeather.titan.setup.portal.editor.PortalDrafts;
import net.onelitefeather.titan.setup.portal.editor.PortalOutline;


import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabelAnchorPreviewTest {

    private static final Vec ANCHOR = new Vec(5, 70, 5);
    private static final Box BOX = new Box(new Vec(0, 64, 0), new Vec(2, 66, 2));

    private static Portal portal(PortalLabel label) {
        return new Portal("p", BOX, "T", null, label);
    }

    @Test
    @DisplayName("Without an anchor there are no anchor points")
    void noAnchorNoPoints() {
        assertTrue(PortalOutline.anchor(null).isEmpty(), "nothing to mark");
    }

    @Test
    @DisplayName("The anchor is a small cross around the position")
    void anchorIsACross() {
        List<Vec> cross = PortalOutline.anchor(ANCHOR);

        assertEquals(PortalOutline.ANCHOR_POINTS, cross.size(), "centre plus six arms");
        assertTrue(cross.contains(ANCHOR), "the anchor itself is marked");
    }

    @Test
    @DisplayName("A portal without label has exactly the shape's outline")
    void portalWithoutLabelKeepsItsOutline() {
        assertEquals(PortalOutline.points(BOX), PortalOutline.points(portal(null)), "outline unchanged");
    }

    @Test
    @DisplayName("A portal with a label anchor adds the cross to its outline")
    void portalWithAnchorAddsTheCross() {
        List<Vec> points = PortalOutline.points(portal(new PortalLabel(ANCHOR, "Hi", null, null, Billboard.CENTER, 0f)));

        assertEquals(PortalOutline.points(BOX).size() + PortalOutline.ANCHOR_POINTS, points.size(), "shape plus cross");
        assertTrue(points.containsAll(PortalOutline.anchor(ANCHOR)), "cross included");
    }

    @Test
    @DisplayName("A label without position adds nothing")
    void labelWithoutPositionAddsNothing() {
        List<Vec> points = PortalOutline.points(portal(new PortalLabel(null, "Hi", null, null, Billboard.CENTER, 0f)));

        assertEquals(PortalOutline.points(BOX), points, "no anchor, no cross");
    }

    @Test
    @DisplayName("The draft preview marks the draft's anchor and follows label here")
    void draftPreviewFollowsTheAnchor() {
        PortalDraft draft = PortalDrafts.cornerOnly("p", new Vec(0, 64, 0));
        Pos player = new Pos(3.5, 65, 2.5);
        List<Vec> withoutAnchor = DraftPoints.of(draft, player, 1.62);

        PortalDrafts.moveLabel(draft, ANCHOR);
        List<Vec> first = DraftPoints.of(draft, player, 1.62);
        Vec moved = new Vec(9, 70, 9);
        PortalDrafts.moveLabel(draft, moved);
        List<Vec> second = DraftPoints.of(draft, player, 1.62);

        assertEquals(withoutAnchor.size() + PortalOutline.ANCHOR_POINTS, first.size(), "cross added once an anchor is set");
        assertTrue(first.containsAll(PortalOutline.anchor(ANCHOR)), "old anchor marked");
        assertTrue(second.containsAll(PortalOutline.anchor(moved)), "new anchor marked");
        assertTrue(!second.contains(ANCHOR), "the old anchor is no longer marked");
    }

    @Test
    @DisplayName("A draft preview with an anchor stays below the sum of both caps")
    void draftPreviewIsBounded() {
        PortalDraft draft = PortalDrafts.withLabelAt(PortalDrafts.ring("p"), ANCHOR);

        int size = DraftPoints.of(draft, new Pos(0, 64, 0), 1.62).size();

        assertTrue(size <= DraftOutline.MAX_POINTS + PortalOutline.ANCHOR_POINTS, "bounded: " + size);
    }
}
