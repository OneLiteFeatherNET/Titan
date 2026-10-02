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

import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The level-up for a new record goes to the runner alone, once per run. */
@ExtendWith(MicrotusExtension.class)
class JumprunRecordSoundTest {

    private static final Pos BYSTANDER_STAND = StartedRun.STAND.add(0, 0, 8);
    private static final int BEST = 12;

    private record Scene(StartedRun run, Collector<SoundEffectPacket> runnerHears,
                         Collector<SoundEffectPacket> bystanderHears) {

        static Scene startWithBest(Env env, JumprunFixture fixture, int best) {
            Instance instance = JumprunFixture.loadedInstance(env);
            return around(env, instance, StartedRun.startWithBest(env, fixture, instance, best));
        }

        static Scene startWithoutBest(Env env, JumprunFixture fixture) {
            Instance instance = JumprunFixture.loadedInstance(env);
            return around(env, instance, StartedRun.start(env, fixture, instance, StartedRun.STAND));
        }

        private static Scene around(Env env, Instance instance, StartedRun run) {
            TestConnection bystander = env.createConnection();
            bystander.connect(instance, BYSTANDER_STAND);
            return new Scene(run, run.connection().trackIncoming(SoundEffectPacket.class), bystander.trackIncoming(SoundEffectPacket.class));
        }

        /** Lands through the ascent and then scores {@code points}. */
        void score(int points) {
            run.landOnNext(JumprunFixture.ASCENT_JUMPS + points);
        }

        /** Counts what the runner hears from now on. */
        Collector<SoundEffectPacket> listen() {
            return run.connection().trackIncoming(SoundEffectPacket.class);
        }

        long levelUps() {
            return levelUps(runnerHears);
        }

        static long levelUps(Collector<SoundEffectPacket> heard) {
            return heard.collect().stream().filter(packet -> packet.soundEvent().equals(SoundEvent.ENTITY_PLAYER_LEVELUP)).count();
        }
    }

    @Test
    void passingTheBestSoundsTheLevelUpExactlyOnce(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.startWithBest(env, fixture, BEST);
            scene.score(BEST);
            Collector<SoundEffectPacket> heard = scene.listen();

            scene.run().landOnNext();

            assertEquals(1, Scene.levelUps(heard), "score 13 passes the best of 12");
        }
    }

    @Test
    void scoresAfterThePassDoNotSoundTheLevelUpAgain(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.startWithBest(env, fixture, BEST);
            scene.score(BEST + 1);
            Collector<SoundEffectPacket> heard = scene.listen();

            scene.run().landOnNext(7);

            assertEquals(0, Scene.levelUps(heard), "scores 14 to 20 are no new event");
        }
    }

    @Test
    void reachingTheBestWithoutPassingItSoundsNoLevelUp(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.startWithBest(env, fixture, BEST);

            scene.score(BEST);

            assertEquals(0, scene.levelUps(), "a tie is no record");
        }
    }

    @Test
    void aBystanderNeverHearsTheLevelUp(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.startWithBest(env, fixture, BEST);

            scene.score(BEST + 1);

            assertTrue(scene.bystanderHears().collect().isEmpty(), "the sound is not instance-wide");
        }
    }

    @Test
    void endingAfterPassingTheBestSoundsNoSecondLevelUp(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.startWithBest(env, fixture, BEST);
            scene.score(BEST + 1);

            fixture.useItem(scene.run().player());

            assertEquals(1, scene.levelUps(), "the pass sounded it, the record message does not repeat it");
        }
    }

    @Test
    void withoutABestTheLevelUpSoundsWhenTheRunEndsWithPoints(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.startWithoutBest(env, fixture);
            scene.score(5);
            assertEquals(0, scene.levelUps(), "nothing to pass during the run");
            Collector<SoundEffectPacket> heard = scene.listen();

            fixture.useItem(scene.run().player());

            assertEquals(1, Scene.levelUps(heard), "the first record sounds with its message");
        }
    }

    @Test
    void withoutABestAScorelessRunSoundsNoLevelUp(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.startWithoutBest(env, fixture);

            fixture.useItem(scene.run().player());

            assertEquals(0, scene.levelUps(), "a run that never scored is no record");
        }
    }

    @Test
    void disconnectingSoundsNoLevelUp(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.startWithoutBest(env, fixture);
            scene.score(5);

            env.process().eventHandler().call(new PlayerDisconnectEvent(scene.run().player()));

            assertEquals(0, scene.levelUps(), "a leaving player is told nothing");
        }
    }
}
