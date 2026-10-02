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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.client.play.ClientPlayerActionPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket;
import net.minestom.server.network.packet.client.play.ClientUseItemPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Clicks on a fake block the way the real client sends them: as packets through the connection,
 * so Minestom's own listeners answer.
 */
@ExtendWith(MicrotusExtension.class)
class JumprunClickTest {

    private static final int SEQUENCE = 7;

    private static ClientPlayerActionPacket dig(ClientPlayerActionPacket.Status status, Point block) {
        return new ClientPlayerActionPacket(status, block, BlockFace.TOP, SEQUENCE);
    }

    private static ClientPlayerBlockPlacementPacket useOn(Point block) {
        return new ClientPlayerBlockPlacementPacket(PlayerHand.MAIN, block, BlockFace.TOP, 0.5f, 1f, 0.5f, false, false, SEQUENCE);
    }

    private static ClientUseItemPacket useItem() {
        return new ClientUseItemPacket(PlayerHand.MAIN, SEQUENCE, 0f, 0f);
    }

    private static void assertStillShown(StartedRun run, Point block, List<BlockChangePacket> sent) {
        BlockChangePacket last = sent.reversed().stream().filter(packet -> packet.blockPosition().sameBlock(block)).findFirst().orElse(null);
        assertTrue(last == null || JumprunFixture.isCourseBlock(last), "the last block packet for the clicked position, if any, shows the run block, but was " + last);
        assertTrue(run.fixture().module().isRunning(run.player()), "the run goes on");
    }

    @Test
    void aCreativePlayerDiggingAtARunBlockKeepsItShown(Env env) {
        digAt(env, GameMode.CREATIVE, ClientPlayerActionPacket.Status.STARTED_DIGGING);
    }

    @Test
    void aSurvivalPlayerDiggingAtARunBlockKeepsItShown(Env env) {
        digAt(env, GameMode.SURVIVAL, ClientPlayerActionPacket.Status.STARTED_DIGGING);
    }

    @Test
    void finishingADigAtARunBlockKeepsItShown(Env env) {
        digAt(env, GameMode.SURVIVAL, ClientPlayerActionPacket.Status.FINISHED_DIGGING);
    }

    @Test
    void cancellingADigAtARunBlockKeepsItShown(Env env) {
        digAt(env, GameMode.SURVIVAL, ClientPlayerActionPacket.Status.CANCELLED_DIGGING);
    }

    @Test
    void usingAnEmptyHandOnARunBlockKeepsItShown(Env env) {
        useAt(env, false, false);
    }

    @Test
    void usingTheJumprunItemOnARunBlockKeepsItShown(Env env) {
        useAt(env, true, false);
    }

    @Test
    void theUsePacketThatFollowsAClickOnARunBlockDoesNotEndTheRun(Env env) {
        useAt(env, true, true);
    }

    @Test
    void usingTheItemInTheAirStillEndsTheRun(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            run.player().setItemInMainHand(fixture.item().itemStack());

            fixture.sendClientPacket(run.player(), useItem());
            env.tick();

            assertFalse(fixture.module().isRunning(run.player()), "the item toggles the run when no block is clicked");
        }
    }

    private static void digAt(Env env, GameMode mode, ClientPlayerActionPacket.Status status) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            run.player().setGameMode(mode);
            Point block = run.ahead().peekFirst().blockPosition();
            Collector<BlockChangePacket> sent = run.connection().trackIncoming(BlockChangePacket.class);

            fixture.sendClientPacket(run.player(), dig(status, block));
            env.tick();

            assertStillShown(run, block, sent.collect());
        }
    }

    /**
     * A right-click as the client sends it: use on the block, then the plain use if nothing took
     * it.
     */
    private static void useAt(Env env, boolean holdingItem, boolean withUsePacket) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            run.player().setItemInMainHand(holdingItem ? fixture.item().itemStack() : ItemStack.AIR);
            Point block = run.ahead().peekFirst().blockPosition();
            Collector<BlockChangePacket> sent = run.connection().trackIncoming(BlockChangePacket.class);

            fixture.sendClientPacket(run.player(), useOn(block));
            if (withUsePacket) {
                fixture.sendClientPacket(run.player(), useItem());
            }
            env.tick();

            assertStillShown(run, block, sent.collect());
        }
    }
}
