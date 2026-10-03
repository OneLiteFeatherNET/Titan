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
package net.onelitefeather.titan.feature.spawn;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.onelitefeather.titan.core.module.LobbyHeightBounds;

/**
 * Teleports a player back to the lobby spawn once they leave the configured height bounds via
 * {@link HeightBounds}. Reads the {@link LobbyHeightBounds} bean live on
 * every move instead of once at construction, so a changed bound applies immediately without a
 * module restart; the startup check in {@link SpawnModule#start()} never re-validates it.
 */
final class SpawnBoundsListener implements Consumer<PlayerMoveEvent> {

    private final Supplier<Pos> spawnPosition;
    private final LobbyHeightBounds lobbyHeightBounds;

    SpawnBoundsListener(Supplier<Pos> spawnPosition, LobbyHeightBounds lobbyHeightBounds) {
        this.spawnPosition = spawnPosition;
        this.lobbyHeightBounds = lobbyHeightBounds;
    }

    @Override
    public void accept(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getInstance() == null) {
            return;
        }
        HeightBounds heightBounds = new HeightBounds(this.lobbyHeightBounds.minHeight(), this.lobbyHeightBounds.maxHeight());
        if (heightBounds.isOutOfBounds(player.getPosition().y())) {
            Optional.ofNullable(this.spawnPosition.get()).ifPresent(player::teleport);
        }
    }
}
