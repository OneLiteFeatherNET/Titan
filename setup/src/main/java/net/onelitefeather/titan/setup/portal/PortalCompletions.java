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

import net.onelitefeather.titan.core.portal.Portal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

/**
 * What tab completion offers for the portal command, free of Minestom. The verbs and {@code box}
 * / {@code ring} are literals or restricted words of the command itself, so Minestom completes
 * them without help.
 */
public final class PortalCompletions {

    private PortalCompletions() {
    }

    /** Ids of the saved portals, then the player's own drafts; each once. */
    public static List<String> ids(List<Portal> portals, List<PortalDraft> drafts) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        portals.forEach(portal -> ids.add(portal.id()));
        drafts.forEach(draft -> ids.add(draft.id()));
        return List.copyOf(ids);
    }

    /** Tasks in use, each once, in list order. */
    public static List<String> tasks(List<Portal> portals) {
        return portals.stream().map(Portal::task).distinct().toList();
    }

    public static List<String> radii() {
        return Stream.concat(Stream.of(1.0), DraftOutline.RADIUS_SUGGESTIONS.stream()).map(PortalCompletions::number).toList();
    }

    public static List<String> permissions() {
        return List.of("none");
    }

    /** Whole numbers without a decimal point, as a player would type them. */
    static String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
