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
package net.onelitefeather.titan.feature.npc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minestom.server.coordinate.Point;

/**
 * Which silhouette shows to which player. A silhouette shows when a player inside the zone comes
 * within {@link #SHOW_RANGE} blocks of its point, and hides when that player comes within
 * {@link #HIDE_RANGE} blocks, leaves the zone or disconnects. A point shows to a player at most
 * once: after it hid, it stays gone for that player.
 */
final class ApparitionRules {

    static final double SHOW_RANGE = 14;
    static final double HIDE_RANGE = 4;

    /** A player's place at one check: whether he is inside the zone and where he stands. */
    record Presence(UUID player, Point position, boolean inside) {
    }

    /** A silhouette of point {@code point} shows to or hides from {@code player}. */
    record Change(UUID player, int point, boolean show) {
    }

    private final List<Point> points;
    private final Map<UUID, Set<Integer>> shown = new HashMap<>();
    private final Map<UUID, Set<Integer>> done = new HashMap<>();

    ApparitionRules(List<Point> points) {
        this.points = List.copyOf(points);
    }

    /** The changes for one check over the players currently in the lobby. */
    List<Change> update(Collection<Presence> presence) {
        List<Change> changes = new ArrayList<>();
        for (Presence player : presence) {
            for (int index = 0; index < this.points.size(); index++) {
                change(player, index).ifPresent(changes::add);
            }
        }
        return changes;
    }

    /**
     * Hides everything a player still sees; called when he disconnects. What he already saw stays
     * remembered for the rest of the session, so a player who is still listed cannot see it again.
     */
    List<Change> forget(UUID player) {
        List<Change> hides = new ArrayList<>();
        Set<Integer> seen = this.shown.remove(player);
        if (seen == null) {
            return hides;
        }
        for (int index : seen) {
            hides.add(new Change(player, index, false));
        }
        this.done.computeIfAbsent(player, key -> new HashSet<>()).addAll(seen);
        return hides;
    }

    private Optional<Change> change(Presence presence, int index) {
        Set<Integer> seen = this.shown.computeIfAbsent(presence.player(), key -> new HashSet<>());
        double distance = presence.position().distance(this.points.get(index));
        if (seen.contains(index)) {
            if (presence.inside() && distance > HIDE_RANGE) {
                return Optional.empty();
            }
            seen.remove(index);
            this.done.computeIfAbsent(presence.player(), key -> new HashSet<>()).add(index);
            return Optional.of(new Change(presence.player(), index, false));
        }
        boolean alreadyShown = this.done.getOrDefault(presence.player(), Set.of()).contains(index);
        if (!presence.inside() || alreadyShown || distance > SHOW_RANGE) {
            return Optional.empty();
        }
        seen.add(index);
        return Optional.of(new Change(presence.player(), index, true));
    }
}
