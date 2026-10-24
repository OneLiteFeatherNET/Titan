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
package net.onelitefeather.titan.feature.throwing;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minestom.server.entity.Player;

/**
 * The vanilla experience bar as the charge meter. It fills while a charge runs, and the thrower's
 * own experience and level come back the moment the charge ends, however it ends.
 */
final class ChargeMeter {

    private record Saved(float experience, int level) {
    }

    private final Map<UUID, Saved> saved = new HashMap<>();

    /** Remembers the thrower's experience before the first fill; a second call changes nothing. */
    void begin(Player thrower) {
        this.saved.putIfAbsent(thrower.getUuid(), new Saved(thrower.getExp(), thrower.getLevel()));
    }

    /** Fills the bar to {@code strength}, from 0 to 1. Does nothing unless {@link #begin} ran. */
    void show(Player thrower, double strength) {
        if (this.saved.containsKey(thrower.getUuid())) {
            thrower.setExp((float) Math.max(0.0, Math.min(1.0, strength)));
        }
    }

    /**
     * Puts the thrower's experience and level back as they were; does nothing if nothing was shown.
     */
    void end(Player thrower) {
        Saved previous = this.saved.remove(thrower.getUuid());
        if (previous != null) {
            thrower.setExp(previous.experience());
            thrower.setLevel(previous.level());
        }
    }

    /** Drops the memory of a player who left; there is nothing to put back on an offline player. */
    void forget(UUID player) {
        this.saved.remove(player);
    }
}
