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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.event.player.PlayerChunkLoadEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Blocks fall into place before they are solid and rise away when they leave the window. */
@ExtendWith(MicrotusExtension.class)
class JumprunBlockAnimationTest {

    private static final Pos BYSTANDER_STAND = StartedRun.STAND.add(0, 0, 8);
    private static final int TICKS = AnimatedBlock.ANIMATION_TICKS;
    private static final Vec HIGH_UP = new Vec(0, 6, 0);

    /** Roughly what a jump takes, well over the length of a fall. */
    private static final int JUMP_TICKS = 12;

    private static void tick(Env env, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            env.tick();
        }
    }

    private static List<Entity> displays(Instance instance) {
        return instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.BLOCK_DISPLAY).toList();
    }

    private static BlockDisplayMeta meta(Entity display) {
        return (BlockDisplayMeta) display.getEntityMeta();
    }

    /** The course blocks the runner is sent while the action runs; a collector reads out once. */
    private static long courseBlocksSentDuring(StartedRun run, Runnable action) {
        Collector<BlockChangePacket> sent = run.connection().trackIncoming(BlockChangePacket.class);
        action.run();
        return sent.collect().stream().filter(JumprunFixture::isCourseBlock).count();
    }

    /**
     * A bystander in the instance and a run that has only just begun, its blocks still in the air.
     */
    private record Scene(Instance instance, Player bystander, TestConnection bystanderConnection,
                         StartedRun run) {

        static Scene startFalling(Env env, JumprunFixture fixture) {
            Instance instance = JumprunFixture.loadedInstance(env);
            TestConnection connection = env.createConnection();
            Player bystander = connection.connect(instance, BYSTANDER_STAND);
            StartedRun run = StartedRun.startFalling(env, fixture, instance, StartedRun.STAND);
            return new Scene(instance, bystander, connection, run);
        }
    }

    @Test
    void aNewBlockStartsHighUpForEveryoneIncludingTheRunner(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.startFalling(env, fixture);

            List<Entity> displays = displays(scene.instance());
            assertEquals(2, displays.size(), "two blocks are shown at the start");
            for (Entity display : displays) {
                assertEquals(HIGH_UP, meta(display).getTranslation(), "the block hangs six blocks above its place");
                assertTrue(display.getViewers().contains(scene.bystander()), "the bystander sees it fall");
                assertTrue(display.getViewers().contains(scene.run().player()), "the runner sees it fall too");
            }
        }
    }

    @Test
    void theRunnerIsSentTheSpawnOfTheFallingBlocks(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Instance instance = JumprunFixture.loadedInstance(env);
            TestConnection connection = env.createConnection();
            Player runner = connection.connect(instance, StartedRun.STAND);
            runner.refreshOnGround(true);
            Collector<SpawnEntityPacket> spawns = connection.trackIncoming(SpawnEntityPacket.class);

            fixture.useItem(runner);

            assertEquals(2, spawns.collect().stream().filter(spawn -> spawn.type() == EntityType.BLOCK_DISPLAY).count(), "the runner sees both blocks come in");
        }
    }

    @Test
    void theBlockFallsFromTheNextTickOnOverEightTicks(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.startFalling(env, fixture);
            Collector<EntityMetaDataPacket> metadata = scene.bystanderConnection().trackIncoming(EntityMetaDataPacket.class);

            env.tick();

            List<EntityMetaDataPacket> sent = metadata.collect();
            for (Entity display : displays(scene.instance())) {
                BlockDisplayMeta meta = meta(display);
                assertEquals(Vec.ZERO, meta.getTranslation(), "the block ends in its place");
                assertEquals(TICKS, meta.getTransformationInterpolationDuration(), "the move takes eight ticks");
                assertEquals(0, meta.getTransformationInterpolationStartDelta(), "the move starts at once");
                assertTrue(sent.stream().anyMatch(packet -> packet.entityId() == display.getEntityId() && packet.entries().values().stream().anyMatch(entry -> Vec.ZERO.equals(entry.value()))), "the bystander is sent the new translation");
            }
        }
    }

    @Test
    void theRunnerGetsTheRealBlocksOnlyOnceTheyHaveFallen(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.startFalling(env, fixture);

            assertEquals(0, courseBlocksSentDuring(scene.run(), () -> tick(env, TICKS)), "nothing is solid while the blocks are falling");
            assertEquals(2, courseBlocksSentDuring(scene.run(), () -> tick(env, 2)), "both blocks are solid when the fall is over");
        }
    }

    @Test
    void theRunnerSeesTheDisplaysDuringTheFallAndNotAfter(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.startFalling(env, fixture);
            Player runner = scene.run().player();

            tick(env, TICKS);
            assertTrue(displays(scene.instance()).stream().allMatch(display -> display.getViewers().contains(runner)), "the runner still sees the blocks fall");

            env.tick();
            assertTrue(displays(scene.instance()).stream().noneMatch(display -> display.getViewers().contains(runner)), "the runner has the real blocks now");
            assertTrue(displays(scene.instance()).stream().allMatch(display -> display.getViewers().contains(scene.bystander())), "the bystander keeps seeing the displays");
        }
    }

    @Test
    void aChunkLoadSendsNoBlockThatIsStillFalling(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.startFalling(env, fixture);
            Entity first = displays(scene.instance()).getFirst();
            int chunkX = first.getPosition().blockX() >> 4;
            int chunkZ = first.getPosition().blockZ() >> 4;

            env.process().eventHandler().call(new PlayerChunkLoadEvent(scene.run().player(), chunkX, chunkZ));

            Runnable chunkLoads = () -> env.process().eventHandler().call(new PlayerChunkLoadEvent(scene.run().player(), chunkX, chunkZ));

            assertEquals(0, courseBlocksSentDuring(scene.run(), chunkLoads), "a block that is not solid yet is not sent");

            tick(env, TICKS + 2);
            assertTrue(courseBlocksSentDuring(scene.run(), chunkLoads) >= 1, "once it has landed it is sent again with the chunk");
        }
    }

    @Test
    void anAbortedRunGivesTheRealBlocksBackAtOnceAndTheDisplaysRiseAway(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Instance instance = JumprunFixture.loadedInstance(env);
            Player bystander = env.createConnection().connect(instance, BYSTANDER_STAND);
            StartedRun run = StartedRun.start(env, fixture, instance, StartedRun.STAND);
            Collector<BlockChangePacket> sent = run.connection().trackIncoming(BlockChangePacket.class);

            fixture.useItem(run.player());

            List<BlockChangePacket> packets = sent.collect();
            assertEquals(2, packets.size(), "both blocks are given back at once");
            assertTrue(packets.stream().noneMatch(JumprunFixture::isCourseBlock), "the runner gets the real blocks back");
            List<Entity> rising = displays(instance);
            assertEquals(2, rising.size(), "the displays stay while they rise");
            for (Entity display : rising) {
                BlockDisplayMeta meta = meta(display);
                assertEquals(HIGH_UP, meta.getTranslation(), "it rises");
                assertEquals(Vec.ONE, meta.getScale(), "without any change of size");
                assertEquals(TICKS, meta.getTransformationInterpolationDuration(), "over eight ticks, as the fall took");
                assertEquals(0, meta.getTransformationInterpolationStartDelta(), "starting at once, as the fall did");
                assertTrue(display.getViewers().contains(run.player()) && display.getViewers().contains(bystander), "everyone sees it rise");
            }

            tick(env, TICKS);
            assertTrue(displays(instance).isEmpty(), "the displays are gone once they have risen");
        }
    }

    @Test
    void aBlockLeavingTheWindowRisesAwayAndIsGoneAfterEightTicks(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Instance instance = JumprunFixture.loadedInstance(env);
            StartedRun run = StartedRun.start(env, fixture, instance, StartedRun.STAND);
            boolean rising = false;
            int shown = 0;

            for (int landing = 0; landing < 4 && !rising; landing++) {
                Collector<ServerPacket> sent = run.connection().trackIncoming();
                shown = displays(instance).size();
                fixture.landOn(run.player(), run.ahead().removeFirst());
                rising = displays(instance).stream().anyMatch(display -> HIGH_UP.equals(meta(display).getTranslation()) && meta(display).getTransformationInterpolationDuration() == TICKS);
                fixture.settle();
                run.learn(sent.collect());
            }

            assertTrue(rising, "a block that left the window rose");
            assertEquals(shown, displays(instance).size(), "the risen display is gone and the new one is there, so the window is as big as before");
        }
    }

    @Test
    void endingDuringTheFallLeavesNoDisplayBehind(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.startFalling(env, fixture);
            tick(env, 3);

            long solid = courseBlocksSentDuring(scene.run(), () -> {
                fixture.useItem(scene.run().player());
                tick(env, TICKS + 2);
            });

            assertTrue(displays(scene.instance()).isEmpty(), "no display survives an end in the middle of the fall");
            assertEquals(0, solid, "a block that was ended while falling never becomes solid");
        }
    }

    @Test
    void shuttingDownRemovesTheDisplaysAtOnceWithoutAnimation(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.startFalling(env, fixture);
            tick(env, 3);

            fixture.stopModule();

            assertTrue(displays(scene.instance()).isEmpty(), "nothing rises, it is gone");
        }
    }

    @Test
    void aRunnerWhoLeavesLeavesNoDisplayBehindAtOnce(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.startFalling(env, fixture);

            scene.run().player().setInstance(env.createFlatInstance(), new Pos(0, 40, 0)).join();

            assertTrue(displays(scene.instance()).isEmpty(), "the blocks of a runner who left are not animated");
        }
    }

    @Test
    void theNextBlockIsAlwaysSolidWhenTheRunnerLandsOnIt(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);

            for (int landing = 1; landing <= 6; landing++) {
                assertEquals(2, run.ahead().size(), "both blocks ahead are solid before landing " + landing);
                Collector<ServerPacket> sent = run.connection().trackIncoming();
                fixture.landOn(run.player(), run.ahead().removeFirst());
                tick(env, JUMP_TICKS);
                run.learn(sent.collect());
            }
        }
    }
}
