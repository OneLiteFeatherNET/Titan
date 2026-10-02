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
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
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

    private static final String ID = "jumprun";
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
    JumprunModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, LobbySpawn spawn) {
        this(titan, spawn, new InMemoryRunRecords(), new RunMessages(), () -> ThreadLocalRandom.current().nextLong());
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
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY).on(PlayerMoveEvent.class, this::onMove).on(PlayerStartFlyingWithElytraEvent.class, event -> endRunOf(event.getPlayer(), EndReason.ELYTRA)).on(PlayerDeathEvent.class, event -> endRunOf(event.getPlayer(), EndReason.DEATH)).on(PlayerDisconnectEvent.class, event -> endRunOf(event.getPlayer(), EndReason.DISCONNECT));
    }

    @PreDestroy
    void stop() {
        this.node.close();
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
        Optional<Run> run = player.isOnGround() ? plan(player) : Optional.empty();
        if (run.isEmpty()) {
            player.sendMessage(messages.noSpace(player.getLocale()));
            LOGGER.atDebug().addKeyValue("player", player.getUuid()).log("jumprun has no room to start");
            return;
        }
        runs.add(run.get());
        fakeBlocks.show(player, run.get().fakeWindow());
        LOGGER.atDebug().addKeyValue("player", player.getUuid()).log("jumprun started");
    }

    private Optional<Run> plan(Player player) {
        Pos feet = player.getPosition();
        // One below the feet, so the assumed top is never above the real surface (a slab, say).
        BlockPos startBlock = new BlockPos(feet.blockX(), feet.blockY() - 1, feet.blockZ());
        Pos spawnPoint = Optional.ofNullable(spawn.position()).orElse(feet);
        Heading heading = Heading.away(feet.x(), feet.z(), spawnPoint.x(), spawnPoint.z(), feet.direction().x(), feet.direction().z());
        RandomGenerator random = RandomGeneratorFactory.of(RANDOM_ALGORITHM).create(seeds.getAsLong());
        return Course.start(feet, startBlock, heading, new InstanceSpaceProbe(player.getInstance()), random).map(course -> new Run(player, course, startBlock));
    }

    private void endRunOf(Player player, EndReason reason) {
        Run run = runs.get(player.getUuid());
        if (run != null) {
            end(run, reason);
        }
    }

    private void onMove(PlayerMoveEvent event) {
        Run run = runs.get(event.getPlayer().getUuid());
        if (run == null) {
            return;
        }
        Course course = run.course();
        Pos to = event.getNewPosition();
        if (course.hasFallen(to.y())) {
            end(run, EndReason.FALL);
        } else if (event.isOnGround()) {
            advance(run, course.advanceTo(to));
        }
    }

    private void advance(Run run, Course.Advance advance) {
        if (advance.jumps() == 0) {
            return;
        }
        Player player = run.player();
        fakeBlocks.reset(player, run.fake(advance.removed()));
        fakeBlocks.show(player, run.fake(advance.added()));
        player.sendActionBar(messages.scoreActionBar(player.getLocale(), run.course().score()));
        if (advance.exhausted()) {
            end(run, EndReason.EXHAUSTED);
        }
    }

    private void end(Run run, EndReason reason) {
        if (!runs.remove(run)) {
            return;
        }
        Player player = run.player();
        int score = run.course().score();
        if (reason.restoresBlocks) {
            fakeBlocks.reset(player, run.fakeWindow());
        }
        boolean isRecord = records.submit(player.getUuid(), score);
        if (reason.announces) {
            // A run that never scored is not worth calling a record, even when it is the first.
            Component message = isRecord && score > 0 ? messages.endRecord(player.getLocale(), score) : messages.endScore(player.getLocale(), score);
            player.sendMessage(message);
        }
        if (reason == EndReason.FALL) {
            player.teleport(run.course().startPoint());
        }
        LOGGER.atDebug().addKeyValue("player", player.getUuid()).log("jumprun ended: reason={}, score={}", reason, score);
    }

    /** Why a run ended, and what the player is owed for it. */
    private enum EndReason {
        ABORT(true, true), FALL(true, true), EXHAUSTED(true, true), ELYTRA(true, true), DEATH(true, true),
        /** The player is gone: nothing to show or tell, but the score stands. */
        DISCONNECT(false, false);

        private final boolean restoresBlocks;
        private final boolean announces;

        EndReason(boolean restoresBlocks, boolean announces) {
            this.restoresBlocks = restoresBlocks;
            this.announces = announces;
        }
    }
}
