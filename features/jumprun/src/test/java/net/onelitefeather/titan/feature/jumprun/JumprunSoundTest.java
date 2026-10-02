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
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The Shepard tone of a point goes to the runner alone. */
@ExtendWith(MicrotusExtension.class)
class JumprunSoundTest {

    private static final Pos BYSTANDER_STAND = StartedRun.STAND.add(0, 0, 8);

    private record Scene(StartedRun run, Player bystander, TestConnection bystanderConnection) {

        static Scene start(Env env, JumprunFixture fixture) {
            Instance instance = JumprunFixture.loadedInstance(env);
            TestConnection connection = env.createConnection();
            Player bystander = connection.connect(instance, BYSTANDER_STAND);
            return new Scene(StartedRun.start(env, fixture, instance, StartedRun.STAND), bystander, connection);
        }
    }

    @Test
    void aPointSoundsTwoComponentsForTheRunner(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);
            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS);

            long sounds = scene.run().landOnNext().stream().filter(SoundEffectPacket.class::isInstance).count();

            assertEquals(2, sounds, "two simultaneous plings");
        }
    }

    @Test
    void aBystanderHearsNothing(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);
            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS);
            Collector<SoundEffectPacket> heard = scene.bystanderConnection().trackIncoming(SoundEffectPacket.class);

            scene.run().landOnNext();

            assertTrue(heard.collect().isEmpty(), "the sound is not instance-wide");
        }
    }

    @Test
    void theAscentIsSilent(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);
            Collector<SoundEffectPacket> heard = scene.run().connection().trackIncoming(SoundEffectPacket.class);

            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS);

            assertTrue(heard.collect().isEmpty(), "ascent jumps do not score");
        }
    }
}
