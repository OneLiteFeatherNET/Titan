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
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.function.Predicate;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Landings on the shapes with two tops or a narrow footprint, as the client reports them. The run
 * is played with the fixed seed of the fixture until the shape is the next block.
 */
@ExtendWith(MicrotusExtension.class)
class JumprunSurfaceLandingTest {

    private static final int MOST_LANDINGS = 400;

    private static long newBlocks(List<ServerPacket> packets) {
        return packets.stream().filter(BlockChangePacket.class::isInstance).map(BlockChangePacket.class::cast).filter(JumprunFixture::isCourseBlock).count();
    }

    private static Block shown(BlockChangePacket packet) {
        return Block.fromStateId(packet.blockStateId());
    }

    private static boolean isStairs(BlockChangePacket packet) {
        return shown(packet).name().endsWith("_stairs");
    }

    private static boolean isCandle(BlockChangePacket packet) {
        return shown(packet).name().endsWith("candle");
    }

    /** Lands on every block ahead until the next one is of the wanted shape. */
    private static StartedRun runUntilNext(Env env, JumprunFixture fixture, Predicate<BlockChangePacket> wanted, String what) {
        StartedRun run = StartedRun.start(env, fixture);
        for (int landing = 0; landing < MOST_LANDINGS; landing++) {
            if (wanted.test(run.ahead().peekFirst())) {
                return run;
            }
            run.landOnNext();
        }
        return fail("no " + what + " came within " + MOST_LANDINGS + " landings");
    }

    /** The client reports feet at {@code at} on the ground and the server settles. */
    private static List<ServerPacket> report(StartedRun run, Pos at) {
        var sent = run.connection().trackIncoming();
        run.fixture().move(run.player(), at, true);
        run.fixture().settle();
        return sent.collect();
    }

    private static Pos cell(BlockChangePacket block, double x, double height, double z) {
        Point at = block.blockPosition();
        return new Pos(at.blockX() + x, at.blockY() + height, at.blockZ() + z);
    }

    /** A point on the low step: the half of the cell away from the side the stairs face. */
    private static Pos lowStep(BlockChangePacket stairs) {
        return switch (shown(stairs).getProperty("facing")) {
            case "north" -> cell(stairs, 0.5, 0.5, 0.9);
            case "south" -> cell(stairs, 0.5, 0.5, 0.1);
            case "east" -> cell(stairs, 0.1, 0.5, 0.5);
            default -> cell(stairs, 0.9, 0.5, 0.5);
        };
    }

    private static Pos highStep(BlockChangePacket stairs) {
        return switch (shown(stairs).getProperty("facing")) {
            case "north" -> cell(stairs, 0.5, 1.0, 0.1);
            case "south" -> cell(stairs, 0.5, 1.0, 0.9);
            case "east" -> cell(stairs, 0.9, 1.0, 0.5);
            default -> cell(stairs, 0.1, 1.0, 0.5);
        };
    }

    @Test
    void landingOnTheLowStepOfStairsMovesTheRunOn(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = runUntilNext(env, fixture, JumprunSurfaceLandingTest::isStairs, "stairs");

            List<ServerPacket> packets = report(run, lowStep(run.ahead().peekFirst()));

            assertEquals(1, newBlocks(packets), "the low step counts as the block");
            assertTrue(fixture.module().isRunning(run.player()), "and the run goes on");
        }
    }

    @Test
    void landingOnTheHighStepOfStairsMovesTheRunOn(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = runUntilNext(env, fixture, JumprunSurfaceLandingTest::isStairs, "stairs");

            List<ServerPacket> packets = report(run, highStep(run.ahead().peekFirst()));

            assertEquals(1, newBlocks(packets), "the high step counts as the block");
            assertTrue(fixture.module().isRunning(run.player()), "and the run goes on");
        }
    }

    @Test
    void aHeightBetweenTheStairStepsIsNoLanding(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = runUntilNext(env, fixture, JumprunSurfaceLandingTest::isStairs, "stairs");

            List<ServerPacket> packets = report(run, cell(run.ahead().peekFirst(), 0.5, 0.75, 0.5));

            assertEquals(0, newBlocks(packets), "neither step is at that height");
            assertTrue(fixture.module().isRunning(run.player()), "it is no fall either");
        }
    }

    @Test
    void besideACandleAtItsHeightIsNoLanding(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = runUntilNext(env, fixture, JumprunSurfaceLandingTest::isCandle, "candle");

            List<ServerPacket> packets = report(run, cell(run.ahead().peekFirst(), 0.95, 0.375, 0.5));

            assertEquals(0, newBlocks(packets), "the hitbox does not reach the candle");
            assertTrue(fixture.module().isRunning(run.player()), "and the run goes on");
        }
    }

    @Test
    void onTheRimOfACandleIsALanding(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = runUntilNext(env, fixture, JumprunSurfaceLandingTest::isCandle, "candle");

            List<ServerPacket> packets = report(run, cell(run.ahead().peekFirst(), 0.86, 0.375, 0.5));

            assertEquals(1, newBlocks(packets), "the hitbox overlaps the candle by a hair");
        }
    }
}
