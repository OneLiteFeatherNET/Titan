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
package net.onelitefeather.titan.feature.portal;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.portal.LobbyPortals;
import net.onelitefeather.titan.core.portal.Portal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends a player to a CloudNet task when they walk or fly through a portal of the lobby world.
 *
 * <p>The portals are read once at start: they are map data, and a change of the map file takes
 * effect with the next restart.
 */
@Singleton
public final class PortalModule {

    static final int EVENT_PRIORITY = 900;

    private static final String ID = "portal";
    private static final Logger LOGGER = LoggerFactory.getLogger(PortalModule.class);

    private final EventNode<Event> titan;
    private final LobbyPortals portals;
    private final Deliver deliver;
    private final PermissionService permissions;
    private final Clock clock;
    private FeatureNode node;

    public PortalModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, LobbyPortals portals, Deliver deliver, PermissionService permissions, Clock clock) {
        this.titan = Objects.requireNonNull(titan, "titan");
        this.portals = Objects.requireNonNull(portals, "portals");
        this.deliver = Objects.requireNonNull(deliver, "deliver");
        this.permissions = Objects.requireNonNull(permissions, "permissions");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @PostConstruct
    void start() {
        List<Portal> all = this.portals.portals();
        LOGGER.info("Portal column started with {} portal(s)", all.size());
        PortalTrigger trigger = new PortalTrigger(new PortalIndex(all), this.permissions, this.clock);
        // The player's current position is still the old one while PlayerMoveEvent runs, so
        // (position, newPosition) is the segment the player is about to travel.
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY).on(PlayerMoveEvent.class, event -> {
            Player player = event.getPlayer();
            trigger.onMove(player.getUuid(), player.getPosition(), event.getNewPosition()).ifPresent(portal -> deliver(player, portal));
        }).on(PlayerDisconnectEvent.class, event -> trigger.forget(event.getPlayer().getUuid()));
    }

    private void deliver(Player player, Portal portal) {
        LOGGER.debug("Player {} entered portal '{}', delivering to task '{}'", player.getUuid(), portal.id(), portal.task());
        this.deliver.sendPlayer(player, DeliverComponent.taskBuilder().taskName(portal.task()).player(player).build());
    }

    @PreDestroy
    void stop() {
        this.node.close();
    }
}
