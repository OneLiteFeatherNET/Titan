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
package net.onelitefeather.titan.feature.tag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers which player tagged which, so the player who was just tagged cannot tag the tagger
 * straight back. Only that pair is affected; every other pair plays on.
 */
final class TagCooldown {

    /** {@code from} tagged {@code to}. */
    private record Pair(UUID from, UUID to) {
    }

    private final Map<Pair, Long> passages = new HashMap<>();

    /** Records that {@code from} passed the role to {@code to} at {@code now}. */
    void passed(UUID from, UUID to, long now) {
        this.passages.values().removeIf(at -> now - at >= TagGame.HIT_COOLDOWN_MILLIS);
        this.passages.put(new Pair(from, to), now);
    }

    /** @return {@code true} if {@code target} tagged {@code attacker} less than the cooldown ago */
    boolean blocks(UUID attacker, UUID target, long now) {
        Long passedAt = this.passages.get(new Pair(target, attacker));
        return passedAt != null && now - passedAt < TagGame.HIT_COOLDOWN_MILLIS;
    }

    /** Drops every pair the player is part of, when the player leaves the game. */
    void forget(UUID player) {
        this.passages.keySet().removeIf(pair -> pair.from().equals(player) || pair.to().equals(player));
    }
}
