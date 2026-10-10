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
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.module.LobbyHeightBounds;
import net.onelitefeather.titan.core.module.LobbySpawn;
import net.onelitefeather.titan.core.module.item.LobbyItems;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Puts a joining player into the lobby and keeps them inside its height bounds: sets the spawning
 * instance and respawn point while a player configures, teleports and equips them with the
 * standard loadout on spawn, and teleports them back to spawn if they fall below or rise above
 * the configured height.
 *
 * <p>Depends on the lobby {@link Instance} and a {@link LobbySpawn} rather than the whole
 * {@code MapProvider}: the spawn position can change after this module is built (e.g. a map
 * reload) while the instance does not, and {@code MapProvider} also carries unrelated concerns
 * this module has no business depending on.
 */
@Singleton
public final class SpawnModule {

    static final int EVENT_PRIORITY = 200;

    private static final String ID = "spawn";

    private final Instance instance;
    private final LobbySpawn spawnPosition;
    private final LobbyHeightBounds heightBounds;
    private final EventNode<Event> titan;
    private final LobbyItems lobbyItems;
    private final Telemetry telemetry;
    private FeatureNode node;

    public SpawnModule(Instance instance, LobbySpawn spawnPosition, LobbyHeightBounds heightBounds, @Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, LobbyItems lobbyItems, Telemetry telemetry) {
        this.instance = Objects.requireNonNull(instance, "instance");
        this.spawnPosition = Objects.requireNonNull(spawnPosition, "spawnPosition");
        this.heightBounds = Objects.requireNonNull(heightBounds, "heightBounds");
        this.titan = Objects.requireNonNull(titan, "titan");
        this.lobbyItems = Objects.requireNonNull(lobbyItems, "lobbyItems");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
    }

    @PostConstruct
    void start() {
        // Abort startup on an invalid value; neither result is kept - the listeners below read
        // the live values again on every join/move.
        SpawnSettings.minHeight(this.heightBounds.minHeight(), this.heightBounds.maxHeight());
        Config.getAs(SpawnSettings.SIMULATION_DISTANCE_KEY, SpawnSettings::simulationDistance);

        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY, this.telemetry).on(AsyncPlayerConfigurationEvent.class, new SpawnConfigurationListener(this.instance, this.spawnPosition::position)).onTraced(PlayerSpawnEvent.class, SpawnTelemetry.JOIN_SPAN, new SpawnJoinListener(this.spawnPosition::position, this.lobbyItems)).on(PlayerMoveEvent.class, new SpawnBoundsListener(this.spawnPosition::position, this.heightBounds, new SpawnTelemetry(this.telemetry)));
    }

    @PreDestroy
    void stop() {
        this.node.close();
    }
}
