/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
 * Fired when a player becomes busy with something that takes them out of the lobby games: a jump
 * and run starts or ends, a hand-over to another server begins. Fired synchronously via {@code
 * EventDispatcher}, so every listener has finished when the caller goes on.
 *
 * <p>Listeners run on the caller's thread: keep them quick and never throw, an exception would
 * abort the caller.
 */
public record LobbyPlayerBusyEvent(Player player, boolean busy) implements PlayerEvent {

    @Override
    public Player getPlayer() {
        return player;
    }
}
