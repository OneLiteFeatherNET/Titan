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

import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The glowing outline around the next block is the runner's alone and moves with each landing. */
@ExtendWith(MicrotusExtension.class)
class JumprunOutlineTest {

    private static final Pos BYSTANDER_STAND = StartedRun.STAND.add(0, 0, 8);
    private static final int GREEN = 0x7CFC00;

    private static Pos at(BlockChangePacket block) {
        return new Pos(block.blockPosition().blockX(), block.blockPosition().blockY(), block.blockPosition().blockZ());
    }

    private static Entity theOutline(Instance instance) {
        List<Entity> outlines = JumprunFixture.outlines(instance);
        assertEquals(1, outlines.size(), "exactly one block is outlined");
        return outlines.getFirst();
    }

    @Test
    void theNextBlockIsOutlinedForTheRunnerAloneOnceItHasLanded(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Instance instance = JumprunFixture.loadedInstance(env);
            Player bystander = env.createConnection().connect(instance, BYSTANDER_STAND);
            StartedRun run = StartedRun.start(env, fixture, instance, StartedRun.STAND);

            Entity outline = theOutline(instance);

            assertEquals(at(run.ahead().peekFirst()), outline.getPosition(), "the outline sits at the next block");
            assertEquals(List.of(run.player()), List.copyOf(outline.getViewers()), "only the runner sees it");
            assertFalse(outline.getViewers().contains(bystander), "the bystander sees no outline");
        }
    }

    @Test
    void theOutlineIsAGlowingGreenSlightlyLargerCopyOfTheBlock(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);

            BlockDisplayMeta meta = (BlockDisplayMeta) theOutline(run.instance()).getEntityMeta();

            assertEquals(new Vec(1.02, 1.02, 1.02), meta.getScale(), "a hair larger than the block");
            assertEquals(new Vec(-0.01, -0.01, -0.01), meta.getTranslation(), "centred on the block");
            assertEquals(GREEN, meta.getGlowColorOverride(), "glows green");
            assertTrue(meta.isHasGlowingEffect(), "glows");
            assertTrue(JumprunFixture.isCourseBlock(run.ahead().peekFirst()) && meta.getBlockStateId().equals(net.minestom.server.instance.block.Block.fromStateId(run.ahead().peekFirst().blockStateId())), "shows the material of the block");
        }
    }

    @Test
    void theOutlineIsLitOnItsOwnNotByTheLightInsideTheBlockItWraps(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);

            BlockDisplayMeta meta = (BlockDisplayMeta) theOutline(run.instance()).getEntityMeta();

            assertNotEquals(-1, meta.getBrightnessOverride(), "the light is set, not sampled inside the block");
        }
    }

    @Test
    void noOutlineExistsWhileTheNextBlockIsStillFalling(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Instance instance = JumprunFixture.loadedInstance(env);
            StartedRun run = StartedRun.startFalling(env, fixture, instance, StartedRun.STAND);

            for (int tick = 0; tick < AnimatedBlock.ANIMATION_TICKS; tick++) {
                env.tick();
                assertTrue(JumprunFixture.outlines(instance).isEmpty(), "nothing is outlined before the block has landed, tick " + tick);
            }
            env.tick();
            env.tick();

            assertEquals(1, JumprunFixture.outlines(instance).size(), "the outline appears once the block has landed");
            assertTrue(JumprunFixture.outlines(instance).getFirst().getViewers().contains(run.player()), "the runner sees it");
        }
    }

    @Test
    void afterALandingTheOutlineIsOnTheNewNextBlockAndTheOldOneIsGone(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Entity before = theOutline(run.instance());

            List<ServerPacket> sent = run.landOnNext();

            Entity after = theOutline(run.instance());
            assertEquals(at(run.ahead().peekFirst()), after.getPosition(), "the outline moved to the new next block");
            assertTrue(before.isRemoved(), "the old outline is gone");
            assertTrue(sent.stream().anyMatch(packet -> packet instanceof SpawnEntityPacket spawn && spawn.entityId() == after.getEntityId()), "the runner is sent the new outline");
        }
    }

    @Test
    void theOutlineKeepsMovingOverSeveralLandings(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);

            for (int landing = 1; landing <= 5; landing++) {
                run.landOnNext();

                assertEquals(at(run.ahead().peekFirst()), theOutline(run.instance()).getPosition(), "outline at the next block after landing " + landing);
            }
        }
    }

    @Test
    void endingTheRunRemovesTheOutlineAtOnce(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            theOutline(run.instance());

            fixture.useItem(run.player());

            assertTrue(JumprunFixture.outlines(run.instance()).isEmpty(), "no outline outlives the run, it does not even rise");
        }
    }

    @Test
    void shuttingDownRemovesTheOutlineAtOnce(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            theOutline(run.instance());

            fixture.stopModule();

            assertTrue(JumprunFixture.outlines(run.instance()).isEmpty(), "no outline survives the shutdown");
        }
    }

    @Test
    void endingDuringTheFallLeavesNoOutlineBehind(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Instance instance = JumprunFixture.loadedInstance(env);
            StartedRun run = StartedRun.startFalling(env, fixture, instance, StartedRun.STAND);

            fixture.useItem(run.player());
            for (int tick = 0; tick < AnimatedBlock.ANIMATION_TICKS + 2; tick++) {
                env.tick();
            }

            assertTrue(JumprunFixture.outlines(instance).isEmpty(), "a block that never landed is never outlined");
        }
    }
}
