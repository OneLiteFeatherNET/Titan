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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** What a second player sees of a run: the blocks as displays that nobody can stand on. */
@ExtendWith(MicrotusExtension.class)
class JumprunSpectatorsTest {

    private static final Pos BYSTANDER_STAND = StartedRun.STAND.add(0, 0, 8);

    /** A runner, a bystander in the same instance and what the runner's screen shows. */
    private record Scene(StartedRun run, Player bystander, Instance instance,
                         Map<BlockPos, Integer> screen) {

        static Scene start(Env env, JumprunFixture fixture) {
            Instance instance = JumprunFixture.loadedInstance(env);
            TestConnection connection = env.createConnection();
            Player bystander = connection.connect(instance, BYSTANDER_STAND);
            StartedRun run = StartedRun.start(env, fixture, instance, StartedRun.STAND);
            Scene scene = new Scene(run, bystander, instance, new HashMap<>());
            scene.apply(List.copyOf(run.ahead()));
            return scene;
        }

        /** The runner lands on the next block; the screen follows what the runner was sent. */
        void landOnNext() {
            apply(run.landOnNext().stream().filter(BlockChangePacket.class::isInstance).map(BlockChangePacket.class::cast).toList());
        }

        void landOnNext(int count) {
            for (int landing = 0; landing < count; landing++) {
                landOnNext();
            }
        }

        private void apply(List<BlockChangePacket> packets) {
            for (BlockChangePacket packet : packets) {
                BlockPos pos = new BlockPos(packet.blockPosition().blockX(), packet.blockPosition().blockY(), packet.blockPosition().blockZ());
                if (JumprunFixture.isCourseBlock(packet)) {
                    screen.put(pos, packet.blockStateId());
                } else {
                    screen.remove(pos);
                }
            }
        }

        /** Block displays in the instance by block position, with their block state id. */
        Map<BlockPos, Integer> displays() {
            Map<BlockPos, Integer> displays = new HashMap<>();
            for (Entity entity : instance.getEntities()) {
                if (entity.getEntityType() == EntityType.BLOCK_DISPLAY) {
                    BlockPos pos = new BlockPos(entity.getPosition().blockX(), entity.getPosition().blockY(), entity.getPosition().blockZ());
                    displays.put(pos, entity.getEntityMeta() instanceof BlockDisplayMeta meta ? meta.getBlockStateId().stateId() : -1);
                }
            }
            return displays;
        }

        List<Entity> displayEntities() {
            return instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.BLOCK_DISPLAY).toList();
        }
    }

    @Test
    void theBystanderSeesADisplayForEachBlockTheRunnerIsShown(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            assertEquals(2, scene.screen().size(), "the runner is shown two blocks at the start");
            assertEquals(scene.screen(), scene.displays(), "a display per shown block, in the same material, at the same place");
            assertTrue(scene.displayEntities().stream().allMatch(display -> display.getViewers().contains(scene.bystander())), "the bystander sees them all");
        }
    }

    @Test
    void theRunnerSeesNoDisplayOfHisOwnBlocks(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            assertFalse(scene.displayEntities().isEmpty(), "there are displays");
            assertTrue(scene.displayEntities().stream().noneMatch(display -> display.getViewers().contains(scene.run().player())), "the runner has his real blocks");
        }
    }

    @Test
    void theBystanderIsSentTheSpawnOfEveryDisplayAndTheRunnerNone(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Instance instance = JumprunFixture.loadedInstance(env);
            TestConnection bystander = env.createConnection();
            bystander.connect(instance, BYSTANDER_STAND);
            Collector<SpawnEntityPacket> seenByBystander = bystander.trackIncoming(SpawnEntityPacket.class);
            TestConnection runner = env.createConnection();
            Player player = runner.connect(instance, StartedRun.STAND);
            player.refreshOnGround(true);
            Collector<SpawnEntityPacket> seenByRunner = runner.trackIncoming(SpawnEntityPacket.class);

            fixture.useItem(player);

            assertEquals(2, displaysIn(seenByBystander.collect()), "two displays are spawned for the bystander");
            assertEquals(0, displaysIn(seenByRunner.collect()), "the runner is told of no display");
        }
    }

    private static long displaysIn(List<SpawnEntityPacket> spawns) {
        return spawns.stream().filter(spawn -> spawn.type() == EntityType.BLOCK_DISPLAY).count();
    }

    @Test
    void noDisplayShowsTheStartBlockWhichIsReal(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);
            BlockPos startBlock = new BlockPos(StartedRun.STAND.blockX(), StartedRun.STAND.blockY() - 1, StartedRun.STAND.blockZ());

            assertFalse(scene.displays().containsKey(startBlock), "the start block is a block of the world, nothing to draw");
        }
    }

    @Test
    void landingMovesTheDisplaysWithTheBlocksOfTheRunner(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            for (int landing = 1; landing <= JumprunFixture.ASCENT_JUMPS + 3; landing++) {
                scene.landOnNext();

                assertEquals(scene.screen(), scene.displays(), "displays match the runner's blocks after landing " + landing);
            }
        }
    }

    @Test
    void aRemovedBlockLeavesNoDisplayBehind(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);
            Set<BlockPos> first = Set.copyOf(scene.displays().keySet());

            scene.landOnNext(5);
            env.tick();

            assertEquals(scene.screen().size(), scene.displayEntities().size(), "exactly one display per shown block, none left of the removed ones");
            assertTrue(Set.copyOf(scene.displays().keySet()).stream().noneMatch(first::contains), "the first two blocks are out of the window by now");
        }
    }

    @Test
    void endingByTheItemRemovesEveryDisplay(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);
            scene.landOnNext(2);

            fixture.useItem(scene.run().player());

            assertTrue(scene.displays().isEmpty(), "no display survives the end");
        }
    }

    @Test
    void endingByAFallRemovesEveryDisplay(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            fixture.move(scene.run().player(), new Pos(0.5, 5.0, 0.5), false);

            assertTrue(scene.displays().isEmpty(), "no display survives the fall");
        }
    }

    @Test
    void endingByElytraRemovesEveryDisplay(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(scene.run().player()));

            assertTrue(scene.displays().isEmpty(), "no display survives the glide");
        }
    }

    @Test
    void disconnectingRemovesEveryDisplay(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            env.process().eventHandler().call(new PlayerDisconnectEvent(scene.run().player()));

            assertTrue(scene.displays().isEmpty(), "no display survives the disconnect");
        }
    }

    @Test
    void leavingTheInstanceRemovesEveryDisplay(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            scene.run().player().setInstance(env.createFlatInstance(), new Pos(0, 40, 0)).join();

            assertTrue(scene.displays().isEmpty(), "no display stays behind in the lobby");
        }
    }

    @Test
    void stoppingTheModuleRemovesEveryDisplay(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            fixture.stopModule();

            assertTrue(scene.displays().isEmpty(), "no display outlives the lobby column");
        }
    }

    @Test
    void theDisplaysAreNoRealBlocksSoNobodyIsStoppedByThem(Env env) {
        // The client simulates players, so a test cannot let the bystander fall through. What the server
        // controls is that the positions stay air, nothing is sent as a block, and displays are not physical.
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Instance instance = JumprunFixture.loadedInstance(env);
            TestConnection bystander = env.createConnection();
            Player other = bystander.connect(instance, BYSTANDER_STAND);
            Collector<BlockChangePacket> blocksForBystander = bystander.trackIncoming(BlockChangePacket.class);

            StartedRun run = StartedRun.start(env, fixture, instance, StartedRun.STAND);
            run.landOnNext(2);

            Scene scene = new Scene(run, other, instance, new HashMap<>());
            assertTrue(blocksForBystander.collect().isEmpty(), "the bystander is sent no block");
            for (BlockPos pos : scene.displays().keySet()) {
                assertEquals(Block.AIR, instance.getBlock(pos.x(), pos.y(), pos.z()), "the world stays air at " + pos);
            }
            assertTrue(scene.displayEntities().stream().noneMatch(Entity::hasPhysics), "a display has no collision of its own");
        }
    }
}
