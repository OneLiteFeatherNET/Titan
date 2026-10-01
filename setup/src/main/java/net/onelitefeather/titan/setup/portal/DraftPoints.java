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
import net.onelitefeather.titan.core.portal.Disc;

import java.util.ArrayList;
import java.util.List;

/** Adapts a {@link PortalDraft} and the player's pose to the pure {@link DraftOutline}. */
final class DraftPoints {

    private DraftPoints() {
    }

    /** The shape's preview plus the label anchor's cross, which follows {@code label here}. */
    static List<Vec> of(PortalDraft draft, Pos position, double eyeHeight) {
        List<Vec> points = new ArrayList<>(shape(draft, position, eyeHeight));
        points.addAll(PortalOutline.anchor(draft.labelPosition()));
        return List.copyOf(points);
    }

    private static List<Vec> shape(PortalDraft draft, Pos position, double eyeHeight) {
        PortalDraft.Form form = draft.form();
        if (form == null) {
            return List.of();
        }
        return switch (form) {
            case BOX -> {
                List<Vec> corners = new ArrayList<>(2);
                if (draft.corner1() != null) {
                    corners.add(draft.corner1());
                }
                if (draft.corner2() != null) {
                    corners.add(draft.corner2());
                }
                yield DraftOutline.box(corners, position);
            }
            case RING -> {
                Disc disc = draft.centre() != null && draft.normal() != null && draft.radius() != null ? new Disc(draft.centre(), draft.radius(), draft.normal()) : null;
                yield DraftOutline.ring(disc, position.add(0, eyeHeight, 0), position.direction());
            }
        };
    }

    /** True while a ring draft has no radius, so the preview shows the default one. */
    static boolean usesDefaultRadius(PortalDraft draft) {
        return draft.form() == PortalDraft.Form.RING && draft.radius() == null;
    }
}
