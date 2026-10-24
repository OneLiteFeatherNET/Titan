/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.feature.tag;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.client.play.ClientInputPacket;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.module.LobbyPlayerBusyEvent;
import net.onelitefeather.titan.core.settings.BooleanSetting;
import net.onelitefeather.titan.core.settings.PlayerSettings;
import net.onelitefeather.titan.core.settings.SettingChangeListener;
import net.onelitefeather.titan.core.settings.SettingsRegistry;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.utils.Cancelable;

/**
 * The tag game: players who switched it on play in every lobby instance, and one of them is the
 * tagger. The rules live in {@link TagGame}; this class connects them to Minestom events and to
 * the player's setting, and {@link TagEffects} shows the result.
 */
@Singleton
public final class TagModule {

    static final int EVENT_PRIORITY = 620;
    static final String ID = "tag";

    /** Ticks between two particle bursts over the tagger. */
    private static final int PARTICLE_TICKS = 10;
    /** Ticks between two looks at the idle rule and two action bar refreshes: one second. */
    private static final int SECOND_TICKS = 20;

    private final EventNode<Event> titan;
    private final Scheduler scheduler;
    private final Clock clock;
    private final Telemetry telemetry;
    private final PlayerSettings settings;
    private final SettingsRegistry registry;
    private final RandomGenerator random;
    private final TagMessages messages = new TagMessages();
    private final TagEffects effects = new TagEffects();
    private final TagTelemetry tagTelemetry;
    private final Map<Instance, TagGame> games = new HashMap<>();
    /** The instance each player takes part in. */
    private final Map<UUID, Instance> members = new HashMap<>();
    /** Every player currently on this lobby, by id: the setting listener needs them by id. */
    private final Map<UUID, Player> players = new HashMap<>();
    private final Set<UUID> busy = new HashSet<>();
    private final Set<UUID> gliding = new HashSet<>();
    private final SettingChangeListener settingChanged = this::onSettingChanged;
    /** The settings have no way to remove a listener; this flag keeps a stopped module silent. */
    private boolean open;
    private FeatureNode node;
    private Task particleTask;
    private Task secondTask;

    @Inject
    public TagModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Scheduler scheduler, Clock clock, Telemetry telemetry, PlayerSettings settings, SettingsRegistry registry) {
        this(titan, scheduler, clock, telemetry, settings, registry, ThreadLocalRandom.current());
    }

    /** The random source picks each new tagger; tests pass a seeded one. */
    TagModule(EventNode<Event> titan, Scheduler scheduler, Clock clock, Telemetry telemetry, PlayerSettings settings, SettingsRegistry registry, RandomGenerator random) {
        this.titan = Objects.requireNonNull(titan, "titan must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
        this.tagTelemetry = new TagTelemetry(this.telemetry);
    }

    @PostConstruct
    void start() {
        this.messages.register();
        this.registry.register(ID, TagSettings.ENABLED);
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY, this.telemetry).on(PlayerSpawnEvent.class, this::onSpawn).on(PlayerDisconnectEvent.class, this::onDisconnect).on(RemoveEntityFromInstanceEvent.class, this::onRemoved).on(EntityAttackEvent.class, this::onAttack).on(EntityDamageEvent.class, this::onDamage).on(PlayerMoveEvent.class, event -> touch(event.getPlayer())).on(PlayerPacketEvent.class, this::onPacket).on(LobbyPlayerBusyEvent.class, this::onBusy).on(PlayerStartFlyingWithElytraEvent.class, event -> gliding.add(event.getPlayer().getUuid())).on(PlayerStopFlyingWithElytraEvent.class, event -> gliding.remove(event.getPlayer().getUuid()));
        this.open = true;
        this.settings.addChangeListener(this.settingChanged);
        this.particleTask = this.scheduler.scheduleTask(this::particles, TaskSchedule.tick(PARTICLE_TICKS), TaskSchedule.tick(PARTICLE_TICKS));
        this.secondTask = this.scheduler.scheduleTask(this::second, TaskSchedule.tick(SECOND_TICKS), TaskSchedule.tick(SECOND_TICKS));
    }

    @PreDestroy
    void stop() {
        this.open = false;
        if (this.node != null) {
            this.node.close();
            this.node = null;
        }
        if (this.particleTask != null) {
            this.particleTask.cancel();
            this.secondTask.cancel();
        }
        for (UUID player : new ArrayList<>(this.members.keySet())) {
            leave(player);
        }
        this.games.clear();
        this.players.clear();
        this.busy.clear();
        this.gliding.clear();
        this.messages.close();
    }

    private void onSpawn(PlayerSpawnEvent event) {
        Player player = event.getPlayer();
        this.players.put(player.getUuid(), player);
        Instance instance = player.getInstance();
        if (instance != null) {
            enterIfEligible(player, instance);
        }
    }

    private void onDisconnect(PlayerDisconnectEvent event) {
        UUID id = event.getPlayer().getUuid();
        leave(id);
        this.players.remove(id);
        this.busy.remove(id);
        this.gliding.remove(id);
    }

    private void onRemoved(RemoveEntityFromInstanceEvent event) {
        if (event.getEntity() instanceof Player player) {
            leave(player.getUuid());
        }
    }

    private void onBusy(LobbyPlayerBusyEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUuid();
        if (event.busy()) {
            this.busy.add(id);
            leave(id);
            return;
        }
        this.busy.remove(id);
        Instance instance = player.getInstance();
        if (instance != null) {
            enterIfEligible(player, instance);
        }
    }

    private void onSettingChanged(UUID player, BooleanSetting setting, boolean previous, boolean current) {
        if (!this.open || !TagSettings.ENABLED.equals(setting)) {
            return;
        }
        this.tagTelemetry.toggled(current);
        if (!current) {
            leave(player);
            return;
        }
        Player online = this.players.get(player);
        if (online != null && online.getInstance() != null) {
            enterIfEligible(online, online.getInstance());
        }
    }

    /** Attacks between two players in the game pass the role if the attacker is the tagger. */
    private void onAttack(EntityAttackEvent event) {
        if (!(event.getEntity() instanceof Player attacker) || !(event.getTarget() instanceof Player target)) {
            return;
        }
        Instance instance = attacker.getInstance();
        TagGame game = instance == null ? null : this.games.get(instance);
        if (game == null || !game.isActive(attacker.getUuid()) || !game.isActive(target.getUuid()) || isGliding(attacker, target)) {
            return;
        }
        Optional<UUID> before = game.tagger();
        TagOutcome outcome = game.hit(attacker.getUuid(), target.getUuid(), now());
        if (outcome == TagOutcome.PASSED) {
            this.tagTelemetry.tagged();
            this.effects.passed(attacker, target);
        } else if (outcome == TagOutcome.COOLDOWN) {
            this.tagTelemetry.cooldown();
            this.effects.cooldown(attacker);
        }
        this.effects.taggerChanged(instance, game, before.orElse(null));
    }

    /**
     * Damage between two players in the game is cancelled: the tag game is a game without harm,
     * and the knockback comes with the damage in Minestom.
     */
    private void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamage().getAttacker() instanceof Player attacker)) {
            return;
        }
        TagGame game = gameOf(victim.getUuid());
        if (game != null && game.isActive(attacker.getUuid()) && game.isActive(victim.getUuid())) {
            Cancelable.cancel(event);
        }
    }

    private void onPacket(PlayerPacketEvent event) {
        if (event.getPacket() instanceof ClientInputPacket) {
            touch(event.getPlayer());
        }
    }

    private void enterIfEligible(Player player, Instance instance) {
        UUID id = player.getUuid();
        if (this.busy.contains(id) || !this.settings.isEnabled(id, TagSettings.ENABLED)) {
            return;
        }
        enter(id, instance);
    }

    private void enter(UUID player, Instance instance) {
        Instance current = this.members.get(player);
        if (instance.equals(current)) {
            return;
        }
        if (current != null) {
            leave(player);
        }
        TagGame game = this.games.computeIfAbsent(instance, ignored -> new TagGame(this.random));
        Optional<UUID> before = game.tagger();
        if (game.join(player, now())) {
            this.members.put(player, instance);
            this.effects.taggerChanged(instance, game, before.orElse(null));
        }
    }

    private void leave(UUID player) {
        Instance instance = this.members.remove(player);
        if (instance == null) {
            return;
        }
        TagGame game = this.games.get(instance);
        Optional<UUID> before = game.tagger();
        game.leave(player, now());
        Player online = this.players.get(player);
        if (online != null) {
            this.effects.leftGame(online);
        }
        this.effects.taggerChanged(instance, game, before.orElse(null));
        if (game.active().isEmpty()) {
            this.games.remove(instance);
        }
    }

    private void touch(Player player) {
        TagGame game = gameOf(player.getUuid());
        if (game != null) {
            game.touch(player.getUuid(), now());
        }
    }

    /** Every second: the idle rule, then a fresh action bar; a handed-over role shows at once. */
    private void second() {
        long now = now();
        for (Map.Entry<Instance, TagGame> entry : this.games.entrySet()) {
            TagGame game = entry.getValue();
            Optional<UUID> before = game.tagger();
            game.tick(now);
            this.effects.taggerChanged(entry.getKey(), game, before.orElse(null));
        }
    }

    private void particles() {
        for (Map.Entry<Instance, TagGame> entry : this.games.entrySet()) {
            this.effects.particles(entry.getKey(), entry.getValue());
        }
    }

    private TagGame gameOf(UUID player) {
        Instance instance = this.members.get(player);
        return instance == null ? null : this.games.get(instance);
    }

    private boolean isGliding(Entity attacker, Entity target) {
        return this.gliding.contains(attacker.getUuid()) || this.gliding.contains(target.getUuid());
    }

    private long now() {
        return this.clock.millis();
    }
}
