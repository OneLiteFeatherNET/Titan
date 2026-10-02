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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Rainbow and Ultra while the runner stands: the blocks change colour or are made anew every
 * {@code jumprun.rerollTicks} (40) standing ticks. The run has already stood for the ticks it took
 * to let the first blocks land when {@link #start} returns, so the margins below are a few ticks.
 */
@ExtendWith(MicrotusExtension.class)
class JumprunRerollTest {

    private static final int INTERVAL = 40;
    private static final int LANDED_AT_START = AnimatedBlock.ANIMATION_TICKS + 2;
    /** Standing ticks after the start that are still short of the interval. */
    private static final int BEFORE_REROLL = INTERVAL - LANDED_AT_START - 8;
    /** Standing ticks after the start that are past the interval, but short of a second one. */
    private static final int PAST_REROLL = INTERVAL + 5;

    private static StartedRun start(Env env, JumprunFixture fixture, Mode mode) {
        return StartedRun.startAfter(env, fixture, player -> choose(fixture, player, mode));
    }

    private static void choose(JumprunFixture fixture, Player player, Mode mode) {
        while (fixture.module().modeOf(player) != mode) {
            player.setSneaking(true);
            fixture.useItem(player);
            player.setSneaking(false);
        }
    }

    private static void tick(Env env, int ticks) {
        for (int i = 0; i < ticks; i++) {
            env.tick();
        }
    }

    private static BlockPos at(BlockChangePacket packet) {
        return new BlockPos(packet.blockPosition().blockX(), packet.blockPosition().blockY(), packet.blockPosition().blockZ());
    }

    private static BlockPos at(Entity entity) {
        return new BlockPos(entity.getPosition().blockX(), entity.getPosition().blockY(), entity.getPosition().blockZ());
    }

    private static List<BlockChangePacket> courseBlocks(List<ServerPacket> packets) {
        return packets.stream().filter(BlockChangePacket.class::isInstance).map(BlockChangePacket.class::cast).filter(JumprunFixture::isCourseBlock).toList();
    }

    private static List<BlockChangePacket> realBlocks(List<ServerPacket> packets) {
        return packets.stream().filter(BlockChangePacket.class::isInstance).map(BlockChangePacket.class::cast).filter(packet -> !JumprunFixture.isCourseBlock(packet)).toList();
    }

    private static Map<BlockPos, BlockChangePacket> byPosition(List<BlockChangePacket> packets) {
        Map<BlockPos, BlockChangePacket> latest = new HashMap<>();
        packets.forEach(packet -> latest.put(at(packet), packet));
        return latest;
    }

    private static Set<BlockPos> placesOfDisplays(StartedRun run) {
        Set<BlockPos> places = new HashSet<>();
        JumprunFixture.blockDisplays(run.instance()).forEach(display -> places.add(at(display)));
        return places;
    }

    private static int stateOf(Entity display) {
        return ((BlockDisplayMeta) display.getEntityMeta()).getBlockStateId().stateId();
    }

    // --- rainbow -----------------------------------------------------------------------------

    @Test
    void rainbowGivesTheBlocksNewMaterialsAtTheSamePlacesWithTheSameShapes(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = start(env, fixture, Mode.RAINBOW);
            Map<BlockPos, BlockChangePacket> before = byPosition(List.copyOf(run.ahead()));
            Collector<ServerPacket> sent = run.connection().trackIncoming();

            tick(env, PAST_REROLL);

            Map<BlockPos, BlockChangePacket> after = byPosition(courseBlocks(sent.collect()));
            assertEquals(before.keySet(), after.keySet(), "the same places");
            before.forEach((pos, old) -> {
                BlockChangePacket recolored = after.get(pos);
                assertNotEquals(old.blockStateId(), recolored.blockStateId(), "a new material at " + pos);
                assertEquals(JumprunFixture.topOf(old), JumprunFixture.topOf(recolored), "the same shape at " + pos);
            });
        }
    }

    @Test
    void rainbowShowsTheNewMaterialsToTheOthersAndOutlinesTheNextBlockInIt(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = start(env, fixture, Mode.RAINBOW);
            Set<BlockPos> places = placesOfDisplays(run);
            Collector<ServerPacket> sent = run.connection().trackIncoming();

            tick(env, PAST_REROLL);

            Map<BlockPos, BlockChangePacket> after = byPosition(courseBlocks(sent.collect()));
            assertEquals(places, placesOfDisplays(run), "no display came or went");
            for (Entity display : JumprunFixture.blockDisplays(run.instance())) {
                assertEquals(after.get(at(display)).blockStateId(), stateOf(display), "the display of " + at(display) + " shows the new material");
            }
            List<Entity> outlines = JumprunFixture.outlines(run.instance());
            assertEquals(1, outlines.size(), "still exactly one outline");
            assertEquals(after.get(at(outlines.getFirst())).blockStateId(), stateOf(outlines.getFirst()), "the outline follows the new material");
        }
    }

    @Test
    void nothingChangesBeforeTheIntervalIsFull(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = start(env, fixture, Mode.RAINBOW);
            Collector<ServerPacket> sent = run.connection().trackIncoming();

            tick(env, BEFORE_REROLL);

            assertTrue(courseBlocks(sent.collect()).isEmpty(), "no block was sent again");
        }
    }

    @Test
    void aLandingBeforeTheIntervalIsFullStartsTheCountAgain(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = start(env, fixture, Mode.RAINBOW);
            Set<BlockPos> shownBefore = byPosition(List.copyOf(run.ahead())).keySet();
            tick(env, BEFORE_REROLL);

            List<ServerPacket> landing = run.settleOnNext();

            assertTrue(courseBlocks(landing).stream().noneMatch(packet -> shownBefore.contains(at(packet))), "the landing recoloured nothing");
            Collector<ServerPacket> quiet = run.connection().trackIncoming();
            tick(env, BEFORE_REROLL);
            assertTrue(courseBlocks(quiet.collect()).isEmpty(), "the count began at the landing");
            Collector<ServerPacket> recoloured = run.connection().trackIncoming();
            tick(env, INTERVAL - BEFORE_REROLL);
            assertFalse(courseBlocks(recoloured.collect()).isEmpty(), "a full interval after the landing recolours");
        }
    }

    // --- ultra -------------------------------------------------------------------------------

    @Test
    void ultraShowsNoOutlineEver(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = start(env, fixture, Mode.ULTRA);

            assertTrue(JumprunFixture.outlines(run.instance()).isEmpty(), "no outline at the start");
            run.landOnNext();
            assertTrue(JumprunFixture.outlines(run.instance()).isEmpty(), "no outline after a landing");
            tick(env, PAST_REROLL);
            fixture.settle();
            assertTrue(JumprunFixture.outlines(run.instance()).isEmpty(), "no outline after a reroll");
        }
    }

    @Test
    void ultraLetsTheOldBlocksRiseAndNewOnesFallInElsewhere(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = start(env, fixture, Mode.ULTRA);
            Set<BlockPos> oldPlaces = placesOfDisplays(run);
            List<Entity> oldDisplays = JumprunFixture.blockDisplays(run.instance());
            Collector<ServerPacket> sent = run.connection().trackIncoming();

            tick(env, PAST_REROLL);
            fixture.settle();

            List<ServerPacket> packets = sent.collect();
            Set<BlockPos> newPlaces = placesOfDisplays(run);
            assertEquals(oldPlaces.size(), newPlaces.size(), "two blocks ahead again");
            assertNotEquals(oldPlaces, newPlaces, "new places");
            assertTrue(oldDisplays.stream().allMatch(Entity::isRemoved), "the old displays are gone after rising");
            assertTrue(byPosition(courseBlocks(packets)).keySet().containsAll(newPlaces), "the runner can stand on the new blocks");
            Set<BlockPos> restored = new HashSet<>();
            realBlocks(packets).forEach(packet -> restored.add(at(packet)));
            Set<BlockPos> dropped = new HashSet<>(oldPlaces);
            dropped.removeAll(newPlaces);
            assertTrue(restored.containsAll(dropped), "the runner sees the real world where an old block was");
        }
    }

    @Test
    void ultraRunsOnOverTheNewBlocks(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = start(env, fixture, Mode.ULTRA);
            Collector<ServerPacket> sent = run.connection().trackIncoming();
            tick(env, PAST_REROLL);
            fixture.settle();
            run.ahead().clear();
            run.learn(sent.collect());

            List<ServerPacket> landing = run.landOnNext();

            assertTrue(fixture.module().isRunning(run.player()), "landing on a new block is no fall");
            assertFalse(courseBlocks(landing).isEmpty(), "the window moved on");
        }
    }

    // --- ending ------------------------------------------------------------------------------

    @Test
    void afterTheRunEndedNoRerollFiresAnymore(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = start(env, fixture, Mode.ULTRA);
            fixture.useItem(run.player());
            fixture.settle();
            Collector<ServerPacket> sent = run.connection().trackIncoming();

            tick(env, PAST_REROLL * 2);

            assertTrue(courseBlocks(sent.collect()).isEmpty(), "no block is sent for a finished run");
            assertTrue(JumprunFixture.blockDisplays(run.instance()).isEmpty(), "no display is made for a finished run");
        }
    }

    @Test
    void afterTheShutdownNoRerollFiresAnymore(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = start(env, fixture, Mode.RAINBOW);
            fixture.stopModule();
            Collector<ServerPacket> sent = run.connection().trackIncoming();

            tick(env, PAST_REROLL * 2);

            assertTrue(courseBlocks(sent.collect()).isEmpty(), "no block is sent after the shutdown");
        }
    }

    @Test
    void otherModesNeverReroll(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = start(env, fixture, Mode.HARD);
            Collector<ServerPacket> sent = run.connection().trackIncoming();

            tick(env, PAST_REROLL * 2);

            assertTrue(courseBlocks(sent.collect()).isEmpty(), "a Hard run stands still");
        }
    }
}
