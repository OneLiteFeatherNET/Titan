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
package net.onelitefeather.titan.runtime.lifecycle;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Spans for a player's configuration, join and disconnect, the matching counters and the online
 * player gauge. Each event happens once per connection, so a span per event stays cheap.
 */
@Singleton
public final class PlayerLifecycle {

    static final String ID = "lifecycle";

    /** Runs before every feature node, so nothing a feature does is missing from the join. */
    static final int EVENT_PRIORITY = 0;

    private final Telemetry telemetry;
    private final EventNode<Event> titan;
    private final IntSupplier onlinePlayerCount;
    // Players counted as joined, so a disconnect without a counted join is not counted either.
    private final Set<UUID> joined = ConcurrentHashMap.newKeySet();
    private FeatureNode node;
    private ObservableLongGauge onlinePlayers;

    public PlayerLifecycle(Telemetry telemetry, @Named(FeatureNode.TITAN_NODE) EventNode<Event> titan) {
        this(telemetry, titan, () -> MinecraftServer.getConnectionManager().getOnlinePlayerCount());
    }

    PlayerLifecycle(Telemetry telemetry, EventNode<Event> titan, IntSupplier onlinePlayerCount) {
        this.telemetry = telemetry;
        this.titan = titan;
        this.onlinePlayerCount = onlinePlayerCount;
    }

    @PostConstruct
    void start() {
        LongCounter joins = this.telemetry.meter().counterBuilder("titan.player.joins").setUnit("{player}").build();
        LongCounter disconnects = this.telemetry.meter().counterBuilder("titan.player.disconnects").setUnit("{player}").build();
        this.onlinePlayers = this.telemetry.meter().gaugeBuilder("titan.players.online").ofLongs().setUnit("{player}").buildWithCallback(measurement -> measurement.record(this.onlinePlayerCount.getAsInt()));
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY, this.telemetry).onTraced(AsyncPlayerConfigurationEvent.class, "player.configure", event -> {
            // Nothing to do: the span around this listener is the point.
        }).onTraced(PlayerSpawnEvent.class, "player.join", event -> {
            // PlayerSpawnEvent also fires on every instance change; only the first one is a join.
            if (event.isFirstSpawn() && this.joined.add(event.getPlayer().getUuid())) {
                joins.add(1);
            }
        }).onTraced(PlayerDisconnectEvent.class, "player.disconnect", event -> {
            if (this.joined.remove(event.getPlayer().getUuid())) {
                disconnects.add(1);
            }
        });
    }

    @PreDestroy
    void stop() {
        this.node.close();
        this.onlinePlayers.close();
    }
}
