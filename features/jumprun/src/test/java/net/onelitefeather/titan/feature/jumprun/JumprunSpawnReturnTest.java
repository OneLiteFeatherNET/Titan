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
import java.util.OptionalInt;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.EntitySoundEffectPacket;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.LobbyReturnToSpawnEvent;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import net.onelitefeather.titan.feature.jumprun.persistence.EndReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Returning to the spawn ends a run like the item does, but the spawn, not the start point, is
 * where the player lands.
 */
@ExtendWith(MicrotusExtension.class)
class JumprunSpawnReturnTest {

    private static final int SCORE = 12;
    private static final int TICKS_FOR_ALL_NOTES = 7;

    private static void returnToSpawn(Env env, Player player) {
        EventDispatcher.call(new LobbyReturnToSpawnEvent(player));
        env.tick();
    }

    @Test
    void theRunEndsWithItsScoreAndTheEndMessage(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            run.landOnNext(JumprunFixture.ASCENT_JUMPS + SCORE);
            Collector<SystemChatPacket> chat = run.connection().trackIncoming(SystemChatPacket.class);

            returnToSpawn(env, run.player());

            assertFalse(fixture.module().isRunning(run.player()), "the run is over");
            assertEquals(OptionalInt.of(SCORE), fixture.records().best(run.player().getUuid(), Mode.MEDIUM), "the score stands");
            Component expected = fixture.messages().endRecord(run.player().getLocale(), Mode.MEDIUM, SCORE);
            chat.assertSingle(packet -> assertEquals(expected, packet.message()));
        }
    }

    @Test
    void noRunBlockStaysVisible(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            run.landOnNext(JumprunFixture.ASCENT_JUMPS + SCORE);
            Collector<BlockChangePacket> sent = run.connection().trackIncoming(BlockChangePacket.class);

            returnToSpawn(env, run.player());

            List<BlockChangePacket> packets = sent.collect();
            assertFalse(packets.isEmpty(), "the window of run blocks is reset");
            assertTrue(packets.stream().noneMatch(JumprunFixture::isCourseBlock), "only real blocks are sent after the end");
        }
    }

    @Test
    void theElytraAndTheHotbarComeBack(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.startAfter(env, fixture, fixture.lobbyItems()::equip);

            returnToSpawn(env, run.player());

            assertEquals(RecordingLobbyItems.ELYTRA, run.player().getEquipment(EquipmentSlot.CHESTPLATE), "the elytra is back");
            assertEquals(RecordingLobbyItems.SLOT_ZERO, run.player().getInventory().getItemStack(0), "the hotbar is back");
        }
    }

    @Test
    void theSidebarIsRemoved(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            SidebarScene scene = SidebarScene.start(env, fixture, JumprunFixture.loadedInstance(env), UUID.randomUUID(), "Alex", Mode.MEDIUM, _ -> {
            });

            returnToSpawn(env, scene.player());

            assertEquals(1, SidebarPackets.removed(scene.collect()).size(), "the sidebar goes with the run");
        }
    }

    @Test
    void theRunnerIsNotSentBackToTheStartPoint(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            run.landOnNext(JumprunFixture.ASCENT_JUMPS + SCORE);
            // The test landing moves no position, so put the runner where a teleport back to the start would show.
            run.player().teleport(StartedRun.STAND.add(2, 0, 0)).join();
            Pos before = run.player().getPosition();
            assertNotEquals(StartedRun.STAND, before, "the runner stands away from the start point");

            returnToSpawn(env, run.player());

            assertEquals(before, run.player().getPosition(), "the spawn teleport follows the event; the run itself does not move the player");
        }
    }

    @Test
    void returningIsNoFailureAndSoundsNoFailTone(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Collector<EntitySoundEffectPacket> heard = run.connection().trackIncoming(EntitySoundEffectPacket.class);

            returnToSpawn(env, run.player());
            for (int tick = 0; tick < TICKS_FOR_ALL_NOTES; tick++) {
                env.tick();
            }

            assertFalse(EndReason.SPAWN_RETURN.failed(), "leaving for the spawn is the player's choice, not a failure");
            assertTrue(heard.collect().stream().noneMatch(packet -> packet.soundEvent().equals(SoundEvent.BLOCK_NOTE_BLOCK_BASS)), "no fail tone");
        }
    }

    @Test
    void aPlayerWithoutARunIsNotAffected(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            TestConnection connection = env.createConnection();
            Player player = connection.connect(JumprunFixture.loadedInstance(env), StartedRun.STAND);
            Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);
            Pos before = player.getPosition();

            returnToSpawn(env, player);

            assertFalse(fixture.module().isRunning(player), "still no run");
            assertEquals(before, player.getPosition(), "not moved");
            assertTrue(fixture.records().best(player.getUuid(), Mode.MEDIUM).isEmpty(), "nothing recorded");
            assertTrue(chat.collect().isEmpty(), "no message");
        }
    }
}
