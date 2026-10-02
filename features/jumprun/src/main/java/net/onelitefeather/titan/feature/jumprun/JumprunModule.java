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
package net.onelitefeather.titan.feature.jumprun;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;
import java.util.function.UnaryOperator;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerChunkLoadEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerStartDiggingEvent;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.module.LobbySpawn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code jumprun} feature: a random jump and run that only the playing player sees, built from
 * fake blocks. All state lives in the {@link RunRegistry}; there is no tick task, work happens in
 * the events of the player who runs.
 *
 * <p>{@code LobbyItems} is not injected: the item calls {@link #toggle(Player)} on this module
 * directly, so there is no dependency on the hotbar column and no cycle with it.
 */
@Singleton
final class JumprunModule {

    /** This feature's position among its sibling {@link FeatureNode}s. */
    static final int EVENT_PRIORITY = 1000;

    static final String ID = "jumprun";
    private static final String RANDOM_ALGORITHM = "L64X128MixRandom";
    private static final Logger LOGGER = LoggerFactory.getLogger(JumprunModule.class);

    private final EventNode<Event> titan;
    private final LobbySpawn spawn;
    private final RunRecords records;
    private final RunMessages messages;
    private final LongSupplier seeds;
    private final RunRegistry runs = new RunRegistry();
    private final FakeBlocks fakeBlocks = new FakeBlocks();
    private FeatureNode node;

    @Inject
    JumprunModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, LobbySpawn spawn, RunRecords records) {
        this(titan, spawn, records, new RunMessages(), () -> ThreadLocalRandom.current().nextLong());
    }

    JumprunModule(EventNode<Event> titan, LobbySpawn spawn, RunRecords records, RunMessages messages, LongSupplier seeds) {
        this.titan = titan;
        this.spawn = spawn;
        this.records = records;
        this.messages = messages;
        this.seeds = seeds;
    }

    @PostConstruct
    void start() {
        this.messages.register();
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY);
        this.node.on(PlayerMoveEvent.class, this::onMove);
        this.node.on(PlayerStartFlyingWithElytraEvent.class, this::onElytra);
        this.node.on(PlayerDeathEvent.class, this::onDeath);
        this.node.on(PlayerChunkLoadEvent.class, this::onChunkLoad);
        this.node.on(PlayerBlockInteractEvent.class, this::onBlockInteract);
        this.node.on(PlayerStartDiggingEvent.class, this::onStartDigging);
        this.node.on(PlayerDisconnectEvent.class, this::onDisconnect);
        this.node.on(RemoveEntityFromInstanceEvent.class, this::onLeaveInstance);
    }

    @PreDestroy
    void stop() {
        // Detach first, so no event can start or move a run while they are ended.
        if (this.node != null) {
            this.node.close();
        }
        runs.all().forEach(run -> end(run, EndReason.SHUTDOWN));
        this.messages.close();
    }

    boolean isRunning(Player player) {
        return runs.get(player.getUuid()) != null;
    }

    /** The item: starts a run, or ends the one that is running. */
    void toggle(Player player) {
        Run running = runs.get(player.getUuid());
        if (running == null) {
            begin(player);
        } else {
            end(running, EndReason.ABORT);
        }
    }

    private void begin(Player player) {
        Optional<Run> planned = player.isOnGround() ? plan(player) : Optional.empty();
        if (planned.isEmpty()) {
            player.sendMessage(messages.noSpace(player.getLocale()));
            LOGGER.atDebug().addKeyValue("player", player.getUuid()).log("jumprun has no room to start");
            return;
        }
        Run run = planned.get();
        synchronized (run) {
            runs.add(run);
            fakeBlocks.show(player, run.fakeWindow());
        }
        LOGGER.atDebug().addKeyValue("player", player.getUuid()).log("jumprun started");
    }

    private Optional<Run> plan(Player player) {
        Pos feet = player.getPosition();
        // One below the feet, so the assumed top is never above the real surface (a slab, say).
        BlockPos startBlock = new BlockPos(feet.blockX(), feet.blockY() - 1, feet.blockZ());
        Pos spawnPoint = Optional.ofNullable(spawn.position()).orElse(feet);
        Heading heading = Heading.away(feet.x(), feet.z(), spawnPoint.x(), spawnPoint.z(), feet.direction().x(), feet.direction().z());
        RandomGenerator random = RandomGeneratorFactory.of(RANDOM_ALGORITHM).create(seeds.getAsLong());
        return Course.start(feet, startBlock, heading, new SpawnZone(spawnPoint.x(), spawnPoint.z()), new InstanceSpaceProbe(player.getInstance()), random).map(course -> new Run(player, course, startBlock));
    }

    private void onElytra(PlayerStartFlyingWithElytraEvent event) {
        endRunOf(event.getPlayer(), EndReason.ELYTRA);
    }

    private void onDeath(PlayerDeathEvent event) {
        endRunOf(event.getPlayer(), EndReason.DEATH);
    }

    private void onDisconnect(PlayerDisconnectEvent event) {
        endRunOf(event.getPlayer(), EndReason.DISCONNECT);
    }

    /**
     * Changing the instance without disconnecting: the client switches world, so there is nothing
     * to send.
     */
    private void onLeaveInstance(RemoveEntityFromInstanceEvent event) {
        if (event.getEntity() instanceof Player player) {
            endRunOf(player, EndReason.LEFT_INSTANCE);
        }
    }

    private void onBlockInteract(PlayerBlockInteractEvent event) {
        resendNextTickIfRunBlock(event.getPlayer(), event.getBlockPosition());
    }

    private void onStartDigging(PlayerStartDiggingEvent event) {
        resendNextTickIfRunBlock(event.getPlayer(), event.getBlockPosition());
    }

    private void endRunOf(Player player, EndReason reason) {
        Run run = runs.get(player.getUuid());
        if (run != null) {
            end(run, reason);
        }
    }

    /** The chunk packet is already out when this fires, so what follows paints over it. */
    private void onChunkLoad(PlayerChunkLoadEvent event) {
        Run run = runs.get(event.getPlayer().getUuid());
        if (run != null) {
            showIfRegistered(run, window -> window.stream().filter(block -> block.pos().x() >> 4 == event.getChunkX() && block.pos().z() >> 4 == event.getChunkZ()).toList());
        }
    }

    /**
     * Minestom answers a click or dig on a block with the real one, after the event; only a packet
     * sent on the next tick lands behind that answer.
     */
    private void resendNextTickIfRunBlock(Player player, Point block) {
        Run run = runs.get(player.getUuid());
        if (run != null && run.showsFakeBlockAt(block.blockX(), block.blockY(), block.blockZ())) {
            player.scheduler().scheduleNextTick(() -> showIfRegistered(run, window -> window));
        }
    }

    /**
     * Shows the chosen part of the window unless the run ended meanwhile, so no block outlives its
     * reset.
     */
    private void showIfRegistered(Run run, UnaryOperator<List<CourseBlock>> part) {
        synchronized (run) {
            if (runs.get(run.player().getUuid()) == run) {
                fakeBlocks.show(run.player(), part.apply(run.fakeWindow()));
            }
        }
    }

    private void onMove(PlayerMoveEvent event) {
        Run run = runs.get(event.getPlayer().getUuid());
        if (run == null) {
            return;
        }
        Pos to = event.getNewPosition();
        synchronized (run) {
            if (runs.get(run.player().getUuid()) != run) {
                return;
            }
            if (run.hasFallen(to.y())) {
                end(run, EndReason.FALL);
            } else if (event.isOnGround()) {
                advance(run, run.advanceTo(to));
            }
        }
    }

    private void advance(Run run, Course.Advance advance) {
        if (advance.jumps() == 0) {
            return;
        }
        Player player = run.player();
        fakeBlocks.reset(player, run.fake(advance.removed()));
        fakeBlocks.show(player, run.fake(advance.added()));
        player.sendActionBar(messages.scoreActionBar(player.getLocale(), run.score()));
        if (advance.exhausted()) {
            end(run, EndReason.EXHAUSTED);
        }
    }

    private void end(Run run, EndReason reason) {
        Player player = run.player();
        int score;
        synchronized (run) {
            if (!runs.remove(run)) {
                return;
            }
            score = run.score();
            if (reason.restoresBlocks()) {
                fakeBlocks.reset(player, run.fakeWindow());
            }
        }
        boolean isRecord = reason.submitsScore() && records.submit(player.getUuid(), score);
        if (reason.announcesScore()) {
            // A run that never scored is not worth calling a record, even when it is the first.
            Component message = isRecord && score > 0 ? messages.endRecord(player.getLocale(), score) : messages.endScore(player.getLocale(), score);
            player.sendMessage(message);
        }
        if (reason == EndReason.FALL) {
            player.teleport(run.startPoint());
        }
        LOGGER.atDebug().addKeyValue("player", player.getUuid()).addKeyValue("reason", reason).addKeyValue("score", score).log("jumprun ended");
    }
}
