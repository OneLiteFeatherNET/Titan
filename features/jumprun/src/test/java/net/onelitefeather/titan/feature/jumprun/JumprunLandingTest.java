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

import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Landings as a real client reports them: the position packet arrives while the player is still in
 * the air, and the ground contact follows in a packet that raises no move event.
 */
@ExtendWith(MicrotusExtension.class)
class JumprunLandingTest {

    private static final int DESCENT_LANDINGS = 80;

    private static long newBlocks(List<ServerPacket> packets) {
        return packets.stream().filter(BlockChangePacket.class::isInstance).map(BlockChangePacket.class::cast).filter(JumprunFixture::isCourseBlock).count();
    }

    @Test
    void landingAndStandingStillCountsTheJumpAndKeepsTheRun(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);

            List<ServerPacket> packets = run.settleOnNext();

            assertEquals(1, newBlocks(packets), "the status-only landing moves the window on");
            assertTrue(fixture.module().isRunning(run.player()), "standing still on the block is no fall");
        }
    }

    @Test
    void severalLandingsReportedOnlyByStatusPacketsNeverEndTheRunOnTheWayDown(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.startIn(env, fixture, Mode.EASY);

            for (int landing = 1; landing <= DESCENT_LANDINGS; landing++) {
                List<ServerPacket> packets = run.settleOnNext();

                assertEquals(1, newBlocks(packets), "landing " + landing + " moves the window on");
                assertTrue(fixture.module().isRunning(run.player()), "landing " + landing + " is no fall");
            }
        }
    }

    @Test
    void aStatusPacketInTheAirOverNoBlockLandsNowhere(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            fixture.sendPositionPacket(run.player(), StartedRun.STAND.add(0, 1.5, 0), false);
            var sent = run.connection().trackIncoming();

            fixture.sendOnGroundPacket(run.player(), true);

            assertTrue(sent.collect().isEmpty(), "ground contact away from every block is no landing");
        }
    }

    @Test
    void aStatusPacketThatSaysTheClientIsInTheAirLandsNowhere(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            BlockChangePacket next = run.ahead().peekFirst();
            fixture.sendPositionPacket(run.player(), new Pos(next.blockPosition().blockX() + 0.5, JumprunFixture.topOf(next), next.blockPosition().blockZ() + 0.5), false);
            var sent = run.connection().trackIncoming();

            fixture.sendOnGroundPacket(run.player(), false);

            assertTrue(sent.collect().isEmpty(), "only a report of ground contact lands");
        }
    }

    @Test
    void aRealFallStillEndsTheRunAfterLandings(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            run.settleOnNext();
            run.settleOnNext();
            Pos onBlock = run.player().getPosition();

            fixture.sendPositionPacket(run.player(), onBlock.add(0, -5, 0), false);

            assertFalse(fixture.module().isRunning(run.player()), "dropping well below the course ends the run");
        }
    }
}
