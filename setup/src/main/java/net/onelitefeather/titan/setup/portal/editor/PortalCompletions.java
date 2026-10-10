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


import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.setup.portal.PortalSources;

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

    /**
     * Words offered in the id position next to the ids; Minestom does not complete literals itself.
     */
    public static List<String> verbs() {
        return List.of("copy", "save-all");
    }

    /** Tasks in use, each once, in list order. */
    public static List<String> tasks(List<Portal> portals) {
        return portals.stream().map(Portal::task).distinct().toList();
    }

    public static List<String> radii() {
        return Stream.concat(Stream.of(1.0), DraftOutline.RADIUS_SUGGESTIONS.stream()).map(PortalCompletions::number).toList();
    }

    /** The label source types; the same vocabulary the map file uses. */
    public static List<String> sourceTypes() {
        return LabelSource.TYPES;
    }

    /** The other worlds that have a map file: what {@code copy <world>} offers. */
    public static List<String> worlds(PortalSources sources) {
        return sources.worlds().stream().filter(world -> !world.equals(sources.active())).toList();
    }

    public static List<String> permissions() {
        return List.of("none");
    }

    /** Whole numbers without a decimal point, as a player would type them. */
    static String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
