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
package net.onelitefeather.titan.app.feature.spawn;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;

/**
 * Teleports a player back to the lobby spawn once they leave the configured height bounds. The
 * actual bounds check is delegated to {@link HeightBounds}, a pure rule this listener merely
 * reacts to - built fresh on every move from {@link SpawnSettings#currentHeightBounds()} rather
 * than once at construction, so a changed {@code spawn.minHeight}/{@code maxHeight} applies to
 * the very next height check, without a module restart (see
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 1).
 */
final class SpawnBoundsListener implements Consumer<PlayerMoveEvent> {

    private final Supplier<Pos> spawnPosition;

    SpawnBoundsListener(Supplier<Pos> spawnPosition) {
        this.spawnPosition = spawnPosition;
    }

    @Override
    public void accept(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getInstance() == null) {
            return;
        }
        SpawnSettings.HeightSettings heightSettings = SpawnSettings.currentHeightBounds();
        HeightBounds heightBounds = new HeightBounds(heightSettings.minHeight(), heightSettings.maxHeight());
        if (heightBounds.isOutOfBounds(player.getPosition().y())) {
            Optional.ofNullable(this.spawnPosition.get()).ifPresent(player::teleport);
        }
    }
}
