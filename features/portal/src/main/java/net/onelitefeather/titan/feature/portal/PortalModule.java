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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.portal.LobbyPortals;
import net.onelitefeather.titan.core.portal.Portal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends a player to a CloudNet task when they walk or fly through a portal of the lobby world, and
 * shows the portals' labels.
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
    private final Instance lobby;
    private final Scheduler scheduler;
    private final Executor reads;
    private final LabelReadings readings;
    private final PortalSettings settings;
    private final List<LabelDisplay> displays = new ArrayList<>();
    private FeatureNode node;
    private LabelRefresh refresh;

    PortalModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, LobbyPortals portals, Deliver deliver, PermissionService permissions, Clock clock, Instance lobby, Scheduler scheduler, @Named(PortalBeans.LABEL_READS) Executor reads, LabelReadings readings, PortalSettings settings) {
        this.titan = Objects.requireNonNull(titan, "titan");
        this.portals = Objects.requireNonNull(portals, "portals");
        this.deliver = Objects.requireNonNull(deliver, "deliver");
        this.permissions = Objects.requireNonNull(permissions, "permissions");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.lobby = Objects.requireNonNull(lobby, "lobby");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.reads = Objects.requireNonNull(reads, "reads");
        this.readings = Objects.requireNonNull(readings, "readings");
        this.settings = Objects.requireNonNull(settings, "settings");
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
        try {
            startLabels(all);
        } catch (RuntimeException e) {
            // A half-started column must not leave displays or the node behind.
            stop();
            throw e;
        }
    }

    private void startLabels(List<Portal> all) {
        List<LabelRefresh.Entry> entries = new ArrayList<>();
        for (Portal portal : all) {
            if (portal.label() != null) {
                LabelDisplay display = LabelDisplay.spawn(this.lobby, portal.label());
                this.displays.add(display);
                entries.add(new LabelRefresh.Entry(portal, portal.label(), display));
            }
        }
        LOGGER.info("Portal labels started with {} label(s)", entries.size());
        if (!entries.isEmpty()) {
            this.refresh = new LabelRefresh(this.scheduler, this.reads, this.readings, entries);
            this.refresh.start(this.settings.labelRefreshSeconds());
        }
    }

    private void deliver(Player player, Portal portal) {
        LOGGER.debug("Player {} entered portal '{}', delivering to task '{}'", player.getUuid(), portal.id(), portal.task());
        this.deliver.sendPlayer(player, DeliverComponent.taskBuilder().taskName(portal.task()).player(player).build());
    }

    @PreDestroy
    void stop() {
        if (this.refresh != null) {
            this.refresh.stop();
        }
        this.displays.forEach(LabelDisplay::remove);
        this.displays.clear();
        if (this.node != null) {
            this.node.close();
        }
    }
}
