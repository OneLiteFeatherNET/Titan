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
package net.onelitefeather.titan.setup.portal.editor;

import net.minestom.server.coordinate.Vec;

public final class PortalDrafts {

    private PortalDrafts() {
    }

    public static PortalDraft empty(String id) {
        return new PortalDraft(id);
    }

    public static PortalDraft cornerOnly(String id, Vec corner1) {
        PortalDraft draft = new PortalDraft(id);
        draft.corner1(corner1);
        return draft;
    }

    public static PortalDraft box(String id, Vec corner1, Vec corner2) {
        PortalDraft draft = cornerOnly(id, corner1);
        draft.corner2(corner2);
        return draft;
    }

    public static PortalDraft ring(String id) {
        PortalDraft draft = new PortalDraft(id);
        draft.form(PortalDraft.Form.RING);
        return draft;
    }

    public static PortalDraft ring(String id, Vec centre, Vec normal, double radius) {
        PortalDraft draft = new PortalDraft(id);
        draft.centre(centre, normal);
        draft.radius(radius);
        return draft;
    }

    public static PortalDraft withLabelAt(PortalDraft draft, Vec position) {
        draft.labelPosition(position);
        return draft;
    }

    public static void moveLabel(PortalDraft draft, Vec position) {
        draft.labelPosition(position);
    }
}
