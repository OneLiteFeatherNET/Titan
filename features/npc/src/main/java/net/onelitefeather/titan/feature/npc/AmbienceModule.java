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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.core.ambience.AmbienceValidator;
import net.onelitefeather.titan.core.ambience.AmbienceZone;
import net.onelitefeather.titan.core.ambience.LobbyAmbiences;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ambience zones of the lobby world: particles, ambient sounds, signs, pages and silhouettes,
 * all from the map file. The zones are read once at start. One scheduled takt drives every zone, so
 * the cost does not grow with the number of entities. An invalid zone is skipped with a warning; an
 * unknown sign key skips only that sign.
 */
@Singleton
public final class AmbienceModule {

    /** This feature's position among its sibling {@link FeatureNode}s. */
    static final int EVENT_PRIORITY = 930;
    /** Ticks between two checks of the zones. */
    static final int TAKT_TICKS = 5;
    /** Players this far outside a zone still see its particles. */
    static final double NEAR_MARGIN = 6;

    private static final String ID = "ambience";
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbienceModule.class);

    private final EventNode<Event> titan;
    private final Instance lobby;
    private final Scheduler scheduler;
    private final LobbyAmbiences ambiences;
    private final Telemetry telemetry;
    private final NpcTelemetry npcTelemetry;
    private final Random random;
    private final List<ZoneRuntime> zones = new ArrayList<>();
    private final List<Entity> decor = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();
    private long takt;
    private volatile boolean stopped;
    private FeatureNode node;

    @Inject
    AmbienceModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Instance lobby, Scheduler scheduler, LobbyAmbiences ambiences, Telemetry telemetry) {
        this(titan, lobby, scheduler, ambiences, telemetry, new Random());
    }

    AmbienceModule(EventNode<Event> titan, Instance lobby, Scheduler scheduler, LobbyAmbiences ambiences, Telemetry telemetry, Random random) {
        this.titan = Objects.requireNonNull(titan, "titan");
        this.lobby = Objects.requireNonNull(lobby, "lobby");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.ambiences = Objects.requireNonNull(ambiences, "ambiences");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
        this.npcTelemetry = new NpcTelemetry(this.telemetry);
        this.random = Objects.requireNonNull(random, "random");
    }

    @PostConstruct
    void start() {
        List<AmbienceZone> usable = usable(this.ambiences.zones());
        LOGGER.info("Ambience column started with {} zone(s)", usable.size());
        try {
            for (AmbienceZone zone : usable) {
                this.zones.add(new ZoneRuntime(zone, this.lobby, this.npcTelemetry, this.random));
                spawnDecor(zone);
            }
            this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY, this.telemetry).on(PlayerDisconnectEvent.class, event -> forget(event.getPlayer().getUuid()));
            this.tasks.add(this.scheduler.scheduleTask(this::tick, TaskSchedule.nextTick(), TaskSchedule.tick(TAKT_TICKS)));
        } catch (RuntimeException e) {
            // A half-started column must not leave entities or the node behind.
            stop();
            throw e;
        }
    }

    @PreDestroy
    void stop() {
        this.stopped = true;
        this.tasks.forEach(Task::cancel);
        this.tasks.clear();
        this.zones.forEach(ZoneRuntime::close);
        this.zones.clear();
        this.decor.forEach(Entity::remove);
        this.decor.clear();
        if (this.node != null) {
            this.node.close();
        }
    }

    static List<AmbienceZone> usable(List<AmbienceZone> all) {
        List<AmbienceZone> usable = new ArrayList<>();
        for (AmbienceZone zone : all) {
            List<String> problems = AmbienceValidator.problems(zone);
            if (!problems.isEmpty()) {
                LOGGER.warn("Skipping ambience zone '{}': {}", zone.id(), String.join("; ", problems));
                continue;
            }
            usable.add(withKnownSigns(zone));
        }
        return usable;
    }

    private static AmbienceZone withKnownSigns(AmbienceZone zone) {
        List<AmbienceZone.Sign> known = new ArrayList<>();
        for (AmbienceZone.Sign sign : zone.signs()) {
            if (NpcMessages.SIGN_KEYS.contains(sign.key())) {
                known.add(sign);
            } else {
                LOGGER.warn("Skipping a sign of ambience zone '{}': unknown text key '{}'", zone.id(), sign.key());
            }
        }
        return new AmbienceZone(zone.id(), zone.area(), zone.particles(), zone.sounds(), known, zone.pages(), zone.apparitions(), zone.apparitionSkin());
    }

    private void tick() {
        if (this.stopped) {
            return;
        }
        this.takt++;
        List<net.minestom.server.entity.Player> players = this.lobby.getPlayers().stream().toList();
        for (ZoneRuntime zone : this.zones) {
            zone.tick(this.takt, players);
        }
    }

    private void forget(UUID player) {
        this.zones.forEach(zone -> zone.forget(player));
    }

    /** The signs and pages of a zone: shared entities, signs in each language, pages as paper. */
    private void spawnDecor(AmbienceZone zone) {
        for (AmbienceZone.Sign sign : zone.signs()) {
            for (Locale language : NpcMessages.LANGUAGES) {
                Entity text = new Entity(EntityType.TEXT_DISPLAY);
                text.setNoGravity(true);
                text.editEntityMeta(TextDisplayMeta.class, meta -> meta.setText(NpcMessages.text(sign.key(), language)));
                text.setInstance(this.lobby, position(sign.position())).join();
                text.updateViewableRule(player -> NpcMessages.languageOf(player.getLocale()) == language);
                this.decor.add(text);
            }
        }
        for (Vec page : zone.pages()) {
            Entity paper = new Entity(EntityType.ITEM_DISPLAY);
            paper.setNoGravity(true);
            paper.editEntityMeta(ItemDisplayMeta.class, meta -> meta.setItemStack(ItemStack.of(Material.PAPER)));
            paper.setInstance(this.lobby, position(page)).join();
            this.decor.add(paper);
        }
    }

    private static Pos position(Vec vec) {
        return new Pos(vec.x(), vec.y(), vec.z());
    }
}
