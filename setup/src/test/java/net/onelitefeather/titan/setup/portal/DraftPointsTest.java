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
package net.onelitefeather.titan.setup.portal;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DraftPointsTest {

    private static final double EYE = 1.62;

    @DisplayName("A draft without a form has nothing to preview")
    @Test
    void formlessDraftHasNoPoints() {
        assertTrue(DraftPoints.of(new PortalDraft("p"), new Pos(0, 64, 0), EYE).isEmpty(), "no form, no points");
    }

    @DisplayName("A box draft with one corner runs to the block the player stands in")
    @Test
    void boxWithOneCornerRunsToThePlayersBlock() {
        PortalDraft draft = new PortalDraft("p");
        draft.corner1(new Vec(0, 64, 0));

        List<Vec> points = DraftPoints.of(draft, new Pos(3.7, 65.2, 2.1), EYE);

        assertEquals(DraftOutline.box(List.of(new Vec(0, 64, 0)), new Vec(3, 65, 2)), points, "same points as the pure outline");
    }

    @DisplayName("A box draft with two corners ignores where the player stands")
    @Test
    void boxWithTwoCornersIgnoresThePlayer() {
        PortalDraft draft = new PortalDraft("p");
        draft.corner1(new Vec(0, 64, 0));
        draft.corner2(new Vec(2, 66, 2));

        assertEquals(DraftPoints.of(draft, new Pos(50, 64, 50), EYE), DraftPoints.of(draft, new Pos(-9, 80, 3), EYE), "the box is fixed by its corners");
    }

    @DisplayName("A ring draft without a radius previews the default radius around the eye")
    @Test
    void ringWithoutRadiusUsesTheDefaultAroundTheEye() {
        PortalDraft draft = new PortalDraft("p");
        draft.form(PortalDraft.Form.RING);
        Pos player = new Pos(10, 64, 10, 0, 0);

        List<Vec> points = DraftPoints.of(draft, player, EYE);

        assertEquals(DraftOutline.ring(null, player.add(0, EYE, 0), player.direction()), points, "default ring around the eye");
    }

    @DisplayName("A ring draft with centre and radius previews that disc, wherever the player is")
    @Test
    void ringWithRadiusPreviewsTheDisc() {
        PortalDraft draft = new PortalDraft("p");
        draft.centre(new Vec(0.5, 70, 0.5), new Vec(0, 0, 1));
        draft.radius(2);

        List<Vec> points = DraftPoints.of(draft, new Pos(99, 64, 99), EYE);

        assertTrue(points.stream().allMatch(point -> Math.abs(point.distance(new Vec(0.5, 70, 0.5)) - 2) < 1e-9), "every point lies on the radius-2 rim");
    }

    @DisplayName("The default-radius state is a ring with a centre missing or a radius missing")
    @Test
    void defaultRadiusStateIsARingWithoutRadius() {
        PortalDraft ring = new PortalDraft("p");
        ring.form(PortalDraft.Form.RING);
        PortalDraft sized = new PortalDraft("q");
        sized.centre(new Vec(0, 0, 0), new Vec(0, 0, 1));
        sized.radius(2);

        assertTrue(DraftPoints.usesDefaultRadius(ring), "ring without a radius");
        assertTrue(!DraftPoints.usesDefaultRadius(sized), "ring with a radius");
        assertTrue(!DraftPoints.usesDefaultRadius(new PortalDraft("r")), "no form");
    }
}
