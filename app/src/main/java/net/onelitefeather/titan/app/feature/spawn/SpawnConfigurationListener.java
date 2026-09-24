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
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.Instance;

/**
 * Sets a joining player's spawning instance and respawn point, mirroring the lobby's former
 * {@code PlayerConfigurationListener}.
 *
 * <p>{@code spawnPosition} is read lazily, once per event, rather than captured once at
 * construction time - the lobby map's spawn point can change after the module was built (e.g. a
 * map reload), and this listener always reflects the current one.
 */
final class SpawnConfigurationListener implements Consumer<AsyncPlayerConfigurationEvent> {

    private final Instance instance;
    private final Supplier<Pos> spawnPosition;

    SpawnConfigurationListener(Instance instance, Supplier<Pos> spawnPosition) {
        this.instance = instance;
        this.spawnPosition = spawnPosition;
    }

    @Override
    public void accept(AsyncPlayerConfigurationEvent event) {
        event.setSpawningInstance(this.instance);
        Optional.ofNullable(this.spawnPosition.get()).ifPresent(event.getPlayer()::setRespawnPoint);
    }
}
