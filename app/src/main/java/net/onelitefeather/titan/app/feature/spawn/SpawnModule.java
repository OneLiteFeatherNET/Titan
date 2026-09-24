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

import java.util.Objects;
import java.util.function.Supplier;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;

/**
 * Puts a joining player into the lobby and keeps them inside its height bounds.
 *
 * <p>Three things, each mirroring one of the lobby's former hand-wired listeners:
 * <ul>
 * <li>sets the spawning instance and respawn point while a player configures ({@code
 * PlayerConfigurationListener}),
 * <li>on spawn, sends the configured simulation distance, teleports the player to the lobby
 * spawn and equips the platform-wide standard loadout ({@code PlayerSpawnListener}),
 * <li>teleports a player back to spawn once they fall below or rise above the configured height
 * ({@code PlayerMoveListener}).
 * </ul>
 *
 * <p>Depends on the lobby {@link Instance} and the current spawn position only - not the whole
 * {@code MapProvider} the lobby previously threaded through these listeners. A {@link Supplier} is
 * enough because the spawn position can change after this module is built (e.g. a map reload)
 * while the instance itself does not, and because {@code MapProvider} also carries unrelated
 * concerns (loading, saving and listing maps) this module has no business depending on. Keeping the
 * constructor to exactly what this module reads follows the Dependency Inversion / Interface
 * Segregation principles this change's platform layer is built around (see {@code design.md},
 * decision 2), and it lets a test hand in a plain {@code () -> pos} instead of building a real map
 * provider.
 */
public final class SpawnModule implements LobbyModule {

    private final Instance instance;
    private final Supplier<Pos> spawnPosition;

    /**
     * @param instance      the instance a configuring player spawns into
     * @param spawnPosition supplies the current lobby spawn position; may return {@code null} if
     *                      the lobby map has none, in which case no respawn point or teleport is
     *                      applied
     */
    public SpawnModule(Instance instance, Supplier<Pos> spawnPosition) {
        this.instance = Objects.requireNonNull(instance, "instance");
        this.spawnPosition = Objects.requireNonNull(spawnPosition, "spawnPosition");
    }

    @Override
    public String id() {
        return "spawn";
    }

    @Override
    public void enable(ModuleContext context) {
        SpawnConfig config = context.config(SpawnConfig.class, SpawnConfig.DEFAULTS);
        HeightBounds heightBounds = new HeightBounds(config.minHeight(), config.maxHeight());
        context.listen(AsyncPlayerConfigurationEvent.class, new SpawnConfigurationListener(this.instance, this.spawnPosition));
        context.listen(PlayerSpawnEvent.class, new SpawnJoinListener(config.simulationDistance(), this.spawnPosition, context.items()));
        context.listen(PlayerMoveEvent.class, new SpawnBoundsListener(heightBounds, this.spawnPosition));
    }
}
