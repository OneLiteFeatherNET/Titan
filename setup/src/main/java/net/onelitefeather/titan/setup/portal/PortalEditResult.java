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
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalProblem;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/** What an edit did; the command turns it into chat text with a {@code switch}. */
public sealed interface PortalEditResult {

    /** A new portal was written to the map. */
    record Saved(Portal portal) implements PortalEditResult {
    }

    /** A portal with the same id was replaced in the map. */
    record Updated(Portal portal) implements PortalEditResult {
    }

    /** The draft is still missing parts. */
    record Pending(String id, List<Missing> missing) implements PortalEditResult {
        public Pending {
            missing = List.copyOf(missing);
        }
    }

    /** The draft can be saved, but has not been. */
    record Complete(String id) implements PortalEditResult {
    }

    /**
     * A label edit; carries the draft's label so the answer can state it. All parts {@code null}
     * means the draft has no label.
     */
    record LabelUpdated(String id, @Nullable Vec position, @Nullable String text,
                        @Nullable String offlineText,
                        @Nullable LabelSource source,
                        List<Missing> missing) implements PortalEditResult {
        public LabelUpdated {
            missing = List.copyOf(missing);
        }

        public boolean hasLabel() {
            return position != null || text != null || offlineText != null || source != null;
        }
    }

    /** The saved portal and the draft are gone. */
    record Removed(String id) implements PortalEditResult {
    }

    /** {@code PortalValidator} found problems for the edited portal; the map is unchanged. */
    record Rejected(String id, List<PortalProblem> problems) implements PortalEditResult {
        public Rejected {
            problems = List.copyOf(problems);
        }
    }

    /** The input broke a rule of the command itself (id pattern, blank task, radius). */
    record Invalid(String reason) implements PortalEditResult {
    }

    /** The draft was discarded. */
    record Cancelled(String id) implements PortalEditResult {
    }

    /** Neither a saved portal nor a draft of the player has this id. */
    record Unknown(String id) implements PortalEditResult {
    }
}
