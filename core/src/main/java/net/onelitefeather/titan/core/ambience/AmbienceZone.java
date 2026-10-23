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
package net.onelitefeather.titan.core.ambience;

import java.util.List;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.npc.NpcSkin;
import net.onelitefeather.titan.core.portal.Box;
import org.jetbrains.annotations.Nullable;

/**
 * An area of the lobby that is made to feel alive: particles, ambient sounds, guiding signs, pages
 * floating by the walls and silhouettes that show up for one player at a time. Plain data read
 * from the map file; {@link AmbienceValidator} decides whether it is usable. Every part is optional
 * and empty when the map file leaves it out.
 *
 * @param id             unique within its world
 * @param area           the box the zone covers, or {@code null} if the map file has none
 * @param particles      the particles drifting through the area, or {@code null} for none
 * @param sounds         the ambient sound played to each player inside, or {@code null} for none
 * @param signs          text displays along the walls
 * @param pages          paper pages floating by the walls
 * @param apparitions    positions where a silhouette may show up for one player
 * @param apparitionSkin the skin of the silhouettes, or {@code null} for the default one
 */
public record AmbienceZone(String id, @Nullable Box area, @Nullable Particles particles,
                           @Nullable Sounds sounds, List<Sign> signs, List<Vec> pages,
                           List<Vec> apparitions, @Nullable NpcSkin apparitionSkin) {

    /**
     * A vanilla particle type drifting through the area.
     *
     * @param type  the vanilla particle name, e.g. {@code white_ash}
     * @param count particles per emission, sent to each player nearby
     */
    public record Particles(String type, int count) {
    }

    /**
     * An ambient sound with a random interval between the bounds.
     *
     * @param key        the vanilla sound name, e.g. {@code ambient.cave}
     * @param minSeconds the shortest interval between two plays, in seconds
     * @param maxSeconds the longest interval between two plays, in seconds
     */
    public record Sounds(String key, double minSeconds, double maxSeconds) {
    }

    /**
     * A text display that shows a translated line in the player's language.
     *
     * @param position where the text floats
     * @param key      the translation key of the line
     */
    public record Sign(Vec position, String key) {
    }
}
