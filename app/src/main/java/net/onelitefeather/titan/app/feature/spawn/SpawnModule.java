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
import io.avaje.inject.Priority;
import jakarta.inject.Singleton;
import java.util.Objects;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.LobbySpawn;
import net.onelitefeather.titan.app.module.ModuleContext;

/**
 * Puts a joining player into the lobby and keeps them inside its height bounds.
 *
 * <p>Three things:
 * <ul>
 * <li>sets the spawning instance and respawn point while a player configures,
 * <li>on spawn, sends the configured simulation distance, teleports the player to the lobby
 * spawn and equips the platform-wide standard loadout,
 * <li>teleports a player back to spawn once they fall below or rise above the configured height.
 * </ul>
 *
 * <p>Depends on the lobby {@link Instance} and the current spawn position only - not the whole
 * {@code MapProvider}. A {@link LobbySpawn} is
 * enough because the spawn position can change after this module is built (e.g. a map reload)
 * while the instance itself does not, and because {@code MapProvider} also carries unrelated
 * concerns (loading, saving and listing maps) this module has no business depending on. Keeping the
 * constructor to exactly what this module reads follows the Dependency Inversion / Interface
 * Segregation principles this change's platform layer is built around (see
 * {@code openspec/changes/avaje-dependency-injection/design.md}, decision 4), and it lets a test
 * hand in a plain {@code () -> pos} instead of building a real map provider. {@link LobbySpawn}
 * rather than a bare {@code Supplier<Pos>} is what makes this bean unambiguous for the dependency
 * injection container to wire - see that decision for why.
 */
@Singleton
@Priority(200)
public final class SpawnModule implements LobbyModule {

    private final Instance instance;
    private final LobbySpawn spawnPosition;

    /**
     * @param instance      the instance a configuring player spawns into
     * @param spawnPosition supplies the current lobby spawn position; may return {@code null} if
     *                      the lobby map has none, in which case no respawn point or teleport is
     *                      applied
     */
    public SpawnModule(Instance instance, LobbySpawn spawnPosition) {
        this.instance = Objects.requireNonNull(instance, "instance");
        this.spawnPosition = Objects.requireNonNull(spawnPosition, "spawnPosition");
    }

    @Override
    public String id() {
        return "spawn";
    }

    @Override
    public void enable(ModuleContext context) {
        int maxHeight = Config.getAs(SpawnSettings.MAX_HEIGHT_KEY, Integer::parseInt);
        int minHeight = SpawnSettings.minHeight(Config.getAs(SpawnSettings.MIN_HEIGHT_KEY, Integer::parseInt), maxHeight);
        int simulationDistance = SpawnSettings.simulationDistance(Config.getAs(SpawnSettings.SIMULATION_DISTANCE_KEY, Integer::parseInt));

        HeightBounds heightBounds = new HeightBounds(minHeight, maxHeight);
        context.listen(AsyncPlayerConfigurationEvent.class, new SpawnConfigurationListener(this.instance, this.spawnPosition::position));
        context.listen(PlayerSpawnEvent.class, new SpawnJoinListener(simulationDistance, this.spawnPosition::position, context.items()));
        context.listen(PlayerMoveEvent.class, new SpawnBoundsListener(heightBounds, this.spawnPosition::position));
    }
}
