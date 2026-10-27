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
package net.onelitefeather.titan.feature.radio;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minestom.server.entity.Player;

/** The radio sessions of the players on this lobby, by player id. */
final class RadioSessions {

    private final Map<UUID, RadioSession> sessions = new ConcurrentHashMap<>();
    private final Supplier<RadioLoad> loads;

    /** @param loads makes the load of a new session, one per player */
    RadioSessions(Supplier<RadioLoad> loads) {
        this.loads = Objects.requireNonNull(loads, "loads must not be null");
    }

    /** The player's session, created on first use. */
    RadioSession of(Player player) {
        return this.sessions.computeIfAbsent(player.getUuid(), ignored -> new RadioSession(player, this.loads.get()));
    }

    Optional<RadioSession> find(UUID player) {
        return Optional.ofNullable(this.sessions.get(player));
    }

    /** Drops the player's session, mode included. */
    void forget(UUID player) {
        this.sessions.remove(player);
    }

    Collection<RadioSession> all() {
        return this.sessions.values();
    }

    void clear() {
        this.sessions.clear();
    }
}
