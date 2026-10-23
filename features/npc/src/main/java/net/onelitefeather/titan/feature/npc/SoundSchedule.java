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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * When an ambient sound plays for each player: a random interval between two bounds, counted in
 * takts of the zone's ticker, so no clock is read. The first play comes after one interval.
 */
final class SoundSchedule {

    private final int minTakts;
    private final int maxTakts;
    private final Random random;
    private final Map<UUID, Long> next = new HashMap<>();

    /** The bounds in takts, from 1 up to {@code maxTakts}, with {@code minTakts <= maxTakts}. */
    SoundSchedule(int minTakts, int maxTakts, Random random) {
        if (minTakts < 1 || maxTakts < minTakts) {
            throw new IllegalArgumentException("takts must satisfy 1 <= min <= max but were " + minTakts + ".." + maxTakts);
        }
        this.minTakts = minTakts;
        this.maxTakts = maxTakts;
        this.random = random;
    }

    /** Whether the player hears the sound at this takt; a play schedules the next one. */
    boolean due(UUID player, long takt) {
        Long due = this.next.get(player);
        if (due == null) {
            this.next.put(player, takt + interval());
            return false;
        }
        if (takt < due) {
            return false;
        }
        this.next.put(player, takt + interval());
        return true;
    }

    /** Forgets a player who left. */
    void forget(UUID player) {
        this.next.remove(player);
    }

    private int interval() {
        return this.minTakts + this.random.nextInt(this.maxTakts - this.minTakts + 1);
    }
}
