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
package net.onelitefeather.titan.core.module.item;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerUseItemEvent;

/**
 * Handles a player using a {@link LobbyItem}, once {@link LobbyItems} has already resolved the
 * used stack's identity tag back to this handler's owning feature.
 */
@FunctionalInterface
public interface ItemUseHandler {

    void handle(Player player, PlayerUseItemEvent event);
}
