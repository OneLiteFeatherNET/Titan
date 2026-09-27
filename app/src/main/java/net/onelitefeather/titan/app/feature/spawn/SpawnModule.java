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
import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Objects;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.onelitefeather.titan.app.module.FeatureNode;
import net.onelitefeather.titan.app.module.LobbySpawn;
import net.onelitefeather.titan.app.module.item.ItemRegistry;

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
 *
 * <p>An {@code @Singleton} bean (see
 * {@code openspec/changes/dissolve-module-platform/design.md}, decision 1): {@link #start()}
 * attaches this feature's own {@link FeatureNode} once the container builds this bean, and
 * {@link #stop()} detaches it again when the container is closed - there is no separate
 * enable/disable step outside the bean lifecycle any more.
 */
@Singleton
public final class SpawnModule {

    static final int EVENT_PRIORITY = 200;

    private final Instance instance;
    private final LobbySpawn spawnPosition;
    private final EventNode<Event> titan;
    private final ItemRegistry itemRegistry;
    private FeatureNode node;

    /**
     * @param instance      the instance a configuring player spawns into
     * @param spawnPosition supplies the current lobby spawn position; may return {@code null} if
     *                      the lobby map has none, in which case no respawn point or teleport is
     *                      applied
     * @param titan         the shared event node this feature's own node attaches under
     * @param itemRegistry  equips the joining player with the platform's standard loadout.
     *                      TODO(dissolve-module-platform, task 3.1): read from the
     *                      {@code LobbyItems} bean directly once every feature's items are beans -
     *                      until then this bridges to items that not-yet-migrated features (e.g.
     *                      navigator, elytra) still register with the old platform, see
     *                      {@code openspec/changes/dissolve-module-platform/tasks.md} execution
     *                      plan
     */
    public SpawnModule(Instance instance, LobbySpawn spawnPosition, @Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, ItemRegistry itemRegistry) {
        this.instance = Objects.requireNonNull(instance, "instance");
        this.spawnPosition = Objects.requireNonNull(spawnPosition, "spawnPosition");
        this.titan = Objects.requireNonNull(titan, "titan");
        this.itemRegistry = Objects.requireNonNull(itemRegistry, "itemRegistry");
    }

    @PostConstruct
    void start() {
        // Abort startup on an invalid value (unchanged behaviour); neither result is kept - the
        // listeners below read the live values again on every join/move (see design.md,
        // decision 1).
        int maxHeightAtStartup = Config.getAs(SpawnSettings.MAX_HEIGHT_KEY, Integer::parseInt);
        SpawnSettings.minHeight(Config.getAs(SpawnSettings.MIN_HEIGHT_KEY, Integer::parseInt), maxHeightAtStartup);
        Config.getAs(SpawnSettings.SIMULATION_DISTANCE_KEY, SpawnSettings::simulationDistance);

        this.node = FeatureNode.attach(this.titan, "spawn", EVENT_PRIORITY).on(AsyncPlayerConfigurationEvent.class, new SpawnConfigurationListener(this.instance, this.spawnPosition::position)).on(PlayerSpawnEvent.class, new SpawnJoinListener(this.spawnPosition::position, this.itemRegistry)).on(PlayerMoveEvent.class, new SpawnBoundsListener(this.spawnPosition::position));
    }

    @PreDestroy
    void stop() {
        this.node.close();
    }
}
