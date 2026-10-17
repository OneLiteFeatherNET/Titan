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

import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * A run is not stopped by the click that started it, nor by the follow-up of a click on the lobby.
 */
@ExtendWith(MicrotusExtension.class)
class JumprunStopTest {

    @Test
    void aUseRightAfterTheStartDoesNotStopTheRun(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.startFalling(env, fixture, JumprunFixture.loadedInstance(env), StartedRun.STAND);

            fixture.useItem(run.player());

            assertTrue(fixture.module().isRunning(run.player()), "a double click is no stop");
        }
    }

    @Test
    void aUseAfterTheGracePeriodStopsTheRun(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.startFalling(env, fixture, JumprunFixture.loadedInstance(env), StartedRun.STAND);
            fixture.tickPastGrace();

            fixture.useItem(run.player());

            assertFalse(fixture.module().isRunning(run.player()), "a deliberate click stops the run");
        }
    }

    @Test
    void theUsePacketThatFollowsAClickOnARealBlockDoesNotStopTheRun(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.startFalling(env, fixture, JumprunFixture.loadedInstance(env), StartedRun.STAND);
            fixture.tickPastGrace();

            Vec ground = new Vec(0, JumprunFixture.GROUND_Y - 1, 0);
            fixture.sendClientPacket(run.player(), new ClientPlayerBlockPlacementPacket(PlayerHand.MAIN, ground, BlockFace.TOP, 0.5f, 1f, 0.5f, false, false, 1));
            fixture.useItem(run.player());

            assertTrue(fixture.module().isRunning(run.player()), "the follow-up of the same click is ignored");
        }
    }
}
