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
package net.onelitefeather.titan.core.module;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;

/**
 * Hook for features that hold per-player state (e.g. a running jump and run) to clean up before
 * the player is moved to spawn. Fired synchronously via {@code EventDispatcher} by {@link
 * SpawnReturn}, so all listeners have finished when the teleport happens.
 *
 * <p>Listeners run on the caller's thread before the teleport: keep them quick and never throw,
 * an exception would abort the return.
 */
public record LobbyReturnToSpawnEvent(Player player) implements PlayerEvent {

    @Override
    public Player getPlayer() {
        return player;
    }
}
