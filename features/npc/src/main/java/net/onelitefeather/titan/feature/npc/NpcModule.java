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
package net.onelitefeather.titan.feature.npc;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.api.deliver.DeliverComponent;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.npc.LobbyNpc;
import net.onelitefeather.titan.core.npc.LobbyNpcs;
import net.onelitefeather.titan.core.npc.NpcValidator;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.portal.TaskJoin;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The join NPCs of the lobby world: a figure that sends a player into a CloudNet task on a left or
 * right click, with the same choice of service as the navigator's quick join. The NPCs are read
 * once at start; a change of the map file takes effect with the next restart. An invalid NPC is
 * skipped with a warning and the others still start.
 */
@Singleton
public final class NpcModule {

    /** This feature's position among its sibling {@link FeatureNode}s. */
    static final int EVENT_PRIORITY = 920;
    static final Duration COOLDOWN = Duration.ofSeconds(2);
    static final double LOOK_RANGE = 8;
    static final int LOOK_PERIOD_TICKS = 5;
    static final int LABEL_PERIOD_TICKS = 100;

    private static final String ID = "npc";
    private static final Logger LOGGER = LoggerFactory.getLogger(NpcModule.class);

    /** A started NPC: its figure and, for a labelled one, the text display above it. */
    private record Npc(LobbyNpc npc, Entity figure, @Nullable Entity label) {
    }

    private final EventNode<Event> titan;
    private final Deliver deliver;
    private final PermissionService permissions;
    private final LobbyNpcs npcs;
    private final Instance lobby;
    private final Scheduler scheduler;
    private final Executor reads;
    private final Optional<PlayerCounts> counts;
    private final ClickCooldown cooldown;
    private final Telemetry telemetry;
    private final NpcTelemetry npcTelemetry;
    private final List<Npc> started = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();
    private final AtomicBoolean reading = new AtomicBoolean();
    private volatile boolean stopped;
    private FeatureNode node;

    NpcModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Deliver deliver, PermissionService permissions, LobbyNpcs npcs, Instance lobby, Scheduler scheduler, @Named(NpcBeans.READS) Executor reads, Optional<PlayerCounts> counts, Clock clock, Telemetry telemetry) {
        this.titan = Objects.requireNonNull(titan, "titan");
        this.deliver = Objects.requireNonNull(deliver, "deliver");
        this.permissions = Objects.requireNonNull(permissions, "permissions");
        this.npcs = Objects.requireNonNull(npcs, "npcs");
        this.lobby = Objects.requireNonNull(lobby, "lobby");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.reads = Objects.requireNonNull(reads, "reads");
        this.counts = Objects.requireNonNull(counts, "counts");
        this.cooldown = new ClickCooldown(Objects.requireNonNull(clock, "clock"), COOLDOWN);
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
        this.npcTelemetry = new NpcTelemetry(this.telemetry);
    }

    @PostConstruct
    void start() {
        List<LobbyNpc> usable = usable(this.npcs.npcs());
        LOGGER.info("Npc column started with {} npc(s)", usable.size());
        try {
            for (LobbyNpc npc : usable) {
                Entity figure = Mannequins.create(npc.skin());
                figure.setInstance(this.lobby, npc.position()).join();
                Entity label = npc.label() == null ? null : NpcLabel.display(this.lobby, npc);
                this.started.add(new Npc(npc, figure, label));
            }
            this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY, this.telemetry).on(PlayerEntityInteractEvent.class, event -> click(event.getPlayer(), event.getTarget())).on(EntityAttackEvent.class, event -> {
                if (event.getEntity() instanceof Player player) {
                    click(player, event.getTarget());
                }
            }).on(PlayerDisconnectEvent.class, event -> this.cooldown.forget(event.getPlayer().getUuid()));
            this.tasks.add(this.scheduler.scheduleTask(this::lookAtPlayers, TaskSchedule.nextTick(), TaskSchedule.tick(LOOK_PERIOD_TICKS)));
            if (this.started.stream().anyMatch(npc -> npc.label() != null)) {
                this.tasks.add(this.scheduler.scheduleTask(this::refreshLabels, TaskSchedule.tick(LABEL_PERIOD_TICKS), TaskSchedule.tick(LABEL_PERIOD_TICKS)));
            }
        } catch (RuntimeException e) {
            // A half-started column must not leave figures or the node behind.
            stop();
            throw e;
        }
    }

    @PreDestroy
    void stop() {
        this.stopped = true;
        this.tasks.forEach(Task::cancel);
        this.tasks.clear();
        for (Npc npc : this.started) {
            npc.figure().remove();
            if (npc.label() != null) {
                npc.label().remove();
            }
        }
        this.started.clear();
        if (this.node != null) {
            this.node.close();
        }
    }

    static List<LobbyNpc> usable(List<LobbyNpc> all) {
        List<LobbyNpc> usable = new ArrayList<>();
        for (LobbyNpc npc : all) {
            List<String> problems = NpcValidator.problems(npc);
            if (problems.isEmpty()) {
                usable.add(npc);
            } else {
                LOGGER.warn("Skipping npc '{}': {}", npc.id(), String.join("; ", problems));
            }
        }
        return usable;
    }

    private void click(Player player, Entity target) {
        Optional<Npc> clicked = this.started.stream().filter(npc -> npc.figure() == target).findFirst();
        if (clicked.isEmpty()) {
            return;
        }
        LobbyNpc npc = clicked.get().npc();
        if (!this.cooldown.tryAcquire(player.getUuid())) {
            this.npcTelemetry.interaction(npc.id(), "cooldown");
            return;
        }
        if (!permitted(player, npc)) {
            this.npcTelemetry.interaction(npc.id(), JoinChoice.Result.DENIED.name().toLowerCase(Locale.ROOT));
            player.sendMessage(NpcMessages.text(NpcMessages.JOIN_DENIED, NpcMessages.languageOf(player.getLocale())));
            return;
        }
        readServices(player, npc);
    }

    private boolean permitted(Player player, LobbyNpc npc) {
        return npc.permission() == null || this.permissions.check(player.getUuid(), npc.permission()) == PermissionResult.ALLOWED;
    }

    // The services are read off the tick, and the decision follows on the next tick with that reading,
    // so the join never acts on a stale or missing reading.
    private void readServices(Player player, LobbyNpc npc) {
        try {
            this.reads.execute(() -> {
                Optional<List<ServiceCount>> services = TaskJoin.services(this.counts, npc.task());
                this.scheduler.scheduleNextTick(() -> conclude(player, npc, services));
            });
        } catch (RuntimeException e) {
            conclude(player, npc, Optional.empty());
        }
    }

    private void conclude(Player player, LobbyNpc npc, Optional<List<ServiceCount>> services) {
        if (this.stopped || !player.isOnline()) {
            return;
        }
        JoinChoice choice = JoinChoice.of(true, services);
        this.npcTelemetry.interaction(npc.id(), choice.result().name().toLowerCase(Locale.ROOT));
        switch (choice.result()) {
            case JOINED ->
                this.deliver.sendPlayer(player, DeliverComponent.serverBuilder().serverName(Objects.requireNonNull(choice.service()).name()).player(player).build());
            case NONE_READY -> player.sendMessage(text(NpcMessages.JOIN_NONE_READY, player));
            case UNAVAILABLE -> player.sendMessage(text(NpcMessages.JOIN_UNAVAILABLE, player));
            case DENIED -> {
                // Checked at the click already.
            }
        }
    }

    private static Component text(String key, Player player) {
        return NpcMessages.text(key, NpcMessages.languageOf(player.getLocale()));
    }

    private void lookAtPlayers() {
        if (this.stopped) {
            return;
        }
        for (Npc npc : this.started) {
            if (!npc.npc().lookAtPlayers()) {
                continue;
            }
            nearest(npc.npc()).ifPresent(npc.figure()::lookAt);
        }
    }

    private Optional<Player> nearest(LobbyNpc npc) {
        return this.lobby.getPlayers().stream().filter(player -> player.getPosition().distance(npc.position()) <= LOOK_RANGE).min((first, second) -> Double.compare(first.getPosition().distance(npc.position()), second.getPosition().distance(npc.position())));
    }

    // The label counts are read off the tick; the texts are applied on the next tick, and a read that
    // is still running when the next period starts is skipped.
    private void refreshLabels() {
        if (this.stopped || !this.reading.compareAndSet(false, true)) {
            return;
        }
        int lobbyPlayers = this.lobby.getPlayers().size();
        try {
            this.reads.execute(() -> {
                try {
                    List<Component> texts = new ArrayList<>();
                    for (Npc npc : this.started) {
                        texts.add(npc.label() == null ? null : NpcLabel.text(npc.npc(), this.counts, lobbyPlayers));
                    }
                    this.scheduler.scheduleNextTick(() -> applyLabels(texts));
                } finally {
                    this.reading.set(false);
                }
            });
        } catch (RuntimeException e) {
            this.reading.set(false);
            LOGGER.warn("Starting the npc label read failed, retrying next period: {}", e.toString());
        }
    }

    private void applyLabels(List<Component> texts) {
        if (this.stopped) {
            return;
        }
        for (int index = 0; index < texts.size(); index++) {
            Npc npc = this.started.get(index);
            if (texts.get(index) != null && npc.label() != null) {
                NpcLabel.show(npc.label(), texts.get(index));
            }
        }
    }
}
