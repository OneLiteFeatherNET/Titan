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
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.packet.server.play.UpdateSimulationDistancePacket;
import net.onelitefeather.titan.app.module.item.ModuleItems;

/**
 * Reacts to a player spawning in the lobby: sends the configured simulation distance, teleports
 * the player to the lobby spawn, and equips them with the platform-wide standard loadout via
 * {@link ModuleItems#equip(Player)}. Reads {@link SpawnSettings#SIMULATION_DISTANCE_KEY} itself,
 * live and unvalidated via {@code Config.getInt(...)}, on every join rather than once at
 * construction - see {@code openspec/changes/config-reload-feature-flags/design.md}, decision 1:
 * a changed value applies to the next player who joins, without a module restart. The strict
 * check in {@link SpawnModule#enable} only ever runs once, at startup (see
 * {@code refactor/drop-runtime-fallback}). Joining is far less frequent than a tick, so building a
 * fresh packet per join (instead of a {@code CachedPacket} built once) is unremarkable.
 */
final class SpawnJoinListener implements Consumer<PlayerSpawnEvent> {

    private final Supplier<Pos> spawnPosition;
    private final ModuleItems items;

    SpawnJoinListener(Supplier<Pos> spawnPosition, ModuleItems items) {
        this.spawnPosition = spawnPosition;
        this.items = items;
    }

    @Override
    public void accept(PlayerSpawnEvent event) {
        Player player = event.getPlayer();
        player.sendPacket(new UpdateSimulationDistancePacket(Config.getInt(SpawnSettings.SIMULATION_DISTANCE_KEY)));
        Optional.ofNullable(this.spawnPosition.get()).ifPresent(player::teleport);
        this.items.equip(player);
    }
}
