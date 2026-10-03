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

/**
 * The single way back to the lobby spawn, shared by the {@code /spawn} command and the navigator
 * so callers depend only on {@code core}.
 *
 * <p>Contract: a {@link LobbyReturnToSpawnEvent} is fired first so features can clean up, then
 * the player is teleported. If the active map has no spawn point, neither happens.
 */
public interface SpawnReturn {

    /** Outcome of a return attempt. */
    enum Result {
        /** The event was fired and the player teleported. */
        RETURNED,
        /** The active map has no spawn point; no event, no teleport. */
        NO_SPAWN
    }

    /** Returns the player to spawn without sending any message. */
    Result sendToSpawn(Player player);

    /**
     * Like {@link #sendToSpawn}, then tells the player the confirmation or the no-spawn message.
     */
    void sendToSpawnAndTell(Player player);
}
