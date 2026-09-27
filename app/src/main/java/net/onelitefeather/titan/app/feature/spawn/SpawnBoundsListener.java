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

import io.avaje.config.Config;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;

/**
 * Teleports a player back to the lobby spawn once they leave the configured height bounds. The
 * actual bounds check is delegated to {@link HeightBounds}, a pure rule this listener merely
 * reacts to - built fresh on every move from {@link SpawnSettings#MIN_HEIGHT_KEY}/
 * {@link SpawnSettings#MAX_HEIGHT_KEY}, read live and unvalidated via
 * {@code Config.getInt(...)}, rather than once at construction, so a changed
 * {@code spawn.minHeight}/{@code maxHeight} applies to the very next height check, without a
 * module restart (see {@code openspec/changes/config-reload-feature-flags/design.md}, decision
 * 1). The cross-field check in {@link SpawnModule#enable} only ever runs once, at startup
 * (see {@code refactor/drop-runtime-fallback}): this per-move read is never re-validated and
 * never falls back to a shipped default.
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
        HeightBounds heightBounds = new HeightBounds(Config.getInt(SpawnSettings.MIN_HEIGHT_KEY), Config.getInt(SpawnSettings.MAX_HEIGHT_KEY));
        if (heightBounds.isOutOfBounds(player.getPosition().y())) {
            Optional.ofNullable(this.spawnPosition.get()).ifPresent(player::teleport);
        }
    }
}
