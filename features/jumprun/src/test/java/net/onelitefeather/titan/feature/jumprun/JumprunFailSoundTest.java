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
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntitySoundEffectPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The sounds of the ascent signal and of a failed run go to the runner alone. */
@ExtendWith(MicrotusExtension.class)
class JumprunFailSoundTest {

    private static final Pos BYSTANDER_STAND = StartedRun.STAND.add(0, 0, 8);
    private static final int NOTES = 3;
    private static final int TICKS_FOR_ALL_NOTES = 7;

    private record Scene(StartedRun run, Collector<EntitySoundEffectPacket> runnerHears,
                         Collector<EntitySoundEffectPacket> bystanderHears) {

        static Scene start(Env env, JumprunFixture fixture) {
            Instance instance = JumprunFixture.loadedInstance(env);
            TestConnection bystander = env.createConnection();
            bystander.connect(instance, BYSTANDER_STAND);
            StartedRun run = StartedRun.start(env, fixture, instance, StartedRun.STAND);
            return new Scene(run, run.connection().trackIncoming(EntitySoundEffectPacket.class), bystander.trackIncoming(EntitySoundEffectPacket.class));
        }

        void fall(JumprunFixture fixture) {
            fixture.move(run.player(), StartedRun.STAND.withY(JumprunFixture.GROUND_Y - 3.5), false);
        }
    }

    private static void tick(Env env, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            env.tick();
        }
    }

    private static List<EntitySoundEffectPacket> bass(Collector<EntitySoundEffectPacket> heard) {
        return heard.collect().stream().filter(packet -> packet.soundEvent().equals(SoundEvent.BLOCK_NOTE_BLOCK_BASS)).toList();
    }

    @Test
    void anAscentLandingSoundsOneSignalForTheRunnerOnly(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            scene.run().landOnNext();

            List<EntitySoundEffectPacket> heard = scene.runnerHears().collect();
            assertEquals(1, heard.size(), "one cue, not the Shepard tone as well");
            assertEquals(SoundEvent.BLOCK_NOTE_BLOCK_HAT, heard.getFirst().soundEvent());
            assertTrue(scene.bystanderHears().collect().isEmpty(), "the signal is not instance-wide");
        }
    }

    @Test
    void aFallSoundsThreeDescendingBassNotesAcrossTheTicks(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            scene.fall(fixture);
            tick(env, TICKS_FOR_ALL_NOTES);

            List<Float> pitches = bass(scene.runnerHears()).stream().map(EntitySoundEffectPacket::pitch).toList();
            assertEquals(List.of(1.0f, 0.84f, 0.67f), pitches);
        }
    }

    @Test
    void aFallWithANewRecordSoundsNoBassNote(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);
            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS + 1);

            scene.fall(fixture);
            tick(env, TICKS_FOR_ALL_NOTES);

            assertTrue(bass(scene.runnerHears()).isEmpty(), "the record is the news, not the failure");
        }
    }

    @Test
    void aFallBelowTheBestStillSoundsAllNotes(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);
            fixture.records().submit(JumprunFixture.finished(scene.run().player().getUuid(), Mode.MEDIUM, 5));
            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS + 1);

            scene.fall(fixture);
            tick(env, TICKS_FOR_ALL_NOTES);

            assertEquals(NOTES, bass(scene.runnerHears()).size());
        }
    }

    @Test
    void everyFallNoteIsBoundToTheRunnersEntity(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            scene.fall(fixture);
            tick(env, TICKS_FOR_ALL_NOTES);

            List<EntitySoundEffectPacket> notes = bass(scene.runnerHears());
            assertEquals(NOTES, notes.size());
            notes.forEach(note -> assertEquals(scene.run().player().getEntityId(), note.entityId(), "the note follows the runner"));
        }
    }

    @Test
    void aFallSoundsTheFirstNoteAtOnce(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            scene.fall(fixture);

            assertEquals(1, bass(scene.runnerHears()).size(), "the others are still scheduled");
        }
    }

    @Test
    void aBystanderDoesNotHearTheFall(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            scene.fall(fixture);
            tick(env, TICKS_FOR_ALL_NOTES);

            assertTrue(scene.bystanderHears().collect().isEmpty(), "the failure sound is private");
        }
    }

    @Test
    void abortingWithTheItemSoundsNoFailure(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            fixture.useItem(scene.run().player());
            tick(env, TICKS_FOR_ALL_NOTES);

            assertTrue(scene.runnerHears().collect().isEmpty(), "an abort is the player's choice");
        }
    }

    @Test
    void disconnectingSoundsNothing(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            env.process().eventHandler().call(new PlayerDisconnectEvent(scene.run().player()));
            tick(env, TICKS_FOR_ALL_NOTES);

            assertTrue(scene.runnerHears().collect().isEmpty(), "nobody is there to hear it");
        }
    }

    @Test
    void aRunnerWhoLeavesAfterTheFallGetsNoLaterNotes(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);
            scene.fall(fixture);

            scene.run().player().remove();
            tick(env, TICKS_FOR_ALL_NOTES);

            assertEquals(1, bass(scene.runnerHears()).size(), "only the note that came before the exit");
        }
    }
}
