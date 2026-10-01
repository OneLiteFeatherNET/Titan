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

import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.PortalLabel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The label part of a {@link PortalDraft}. It counts as set as soon as any part is; only then do
 * position and text become required.
 */
public final class LabelDraft {

    private @Nullable Vec position;
    private @Nullable String text;
    private @Nullable String offlineText;
    private @Nullable LabelSource source;
    // Not editable in-game; kept so editing a saved label does not reset what the map file says.
    private Billboard billboard = Billboard.CENTER;
    private float yaw;

    /** A draft holding the values of a saved label; empty for {@code null}. */
    static LabelDraft of(@Nullable PortalLabel label) {
        LabelDraft draft = new LabelDraft();
        if (label != null) {
            draft.position = label.position();
            draft.text = label.text();
            draft.offlineText = label.offlineText();
            draft.source = label.source();
            draft.billboard = label.billboard();
            draft.yaw = label.yaw();
        }
        return draft;
    }

    public @Nullable Vec position() {
        return position;
    }

    public @Nullable String text() {
        return text;
    }

    public @Nullable String offlineText() {
        return offlineText;
    }

    public @Nullable LabelSource source() {
        return source;
    }

    public boolean isSet() {
        return position != null || text != null || offlineText != null || source != null;
    }

    /** What the label still needs; nothing while no part is set. */
    List<Missing> missing() {
        List<Missing> missing = new ArrayList<>();
        if (isSet()) {
            if (position == null) {
                missing.add(Missing.LABEL_POSITION);
            }
            if (text == null) {
                missing.add(Missing.LABEL_TEXT);
            }
        }
        return missing;
    }

    /** The label to save, or {@code null} while no part is set. */
    @Nullable
    PortalLabel toLabel() {
        return isSet() ? new PortalLabel(position, text, offlineText, source, billboard, yaw) : null;
    }

    void position(Vec position) {
        this.position = position;
    }

    void text(String text) {
        this.text = text;
    }

    void offlineText(String text) {
        this.offlineText = text;
    }

    void source(LabelSource source) {
        this.source = source;
    }

    void remove() {
        position = null;
        text = null;
        offlineText = null;
        source = null;
        billboard = Billboard.CENTER;
        yaw = 0;
    }
}
