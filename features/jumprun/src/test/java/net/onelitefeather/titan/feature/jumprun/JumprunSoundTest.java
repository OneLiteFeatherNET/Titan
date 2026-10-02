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
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.EntitySoundEffectPacket;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.server.sound.SoundEvent;
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

            long sounds = scene.run().landOnNext().stream().filter(EntitySoundEffectPacket.class::isInstance).count();

            assertEquals(2, sounds, "two simultaneous plings");
        }
    }

    @Test
    void thePointSoundsAreBoundToTheRunnersEntity(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);
            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS);

            List<ServerPacket> landing = scene.run().landOnNext();

            landing.stream().filter(EntitySoundEffectPacket.class::isInstance).map(EntitySoundEffectPacket.class::cast).forEach(packet -> assertEquals(scene.run().player().getEntityId(), packet.entityId(), "the tone follows the runner"));
            assertTrue(landing.stream().noneMatch(SoundEffectPacket.class::isInstance), "no positional sound");
        }
    }

    @Test
    void aBystanderHearsNothing(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);
            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS);
            Collector<EntitySoundEffectPacket> heard = scene.bystanderConnection().trackIncoming(EntitySoundEffectPacket.class);

            scene.run().landOnNext();

            assertTrue(heard.collect().isEmpty(), "the sound is not instance-wide");
        }
    }

    @Test
    void anAscentLandingSoundsNoPointTone(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);
            Collector<EntitySoundEffectPacket> heard = scene.run().connection().trackIncoming(EntitySoundEffectPacket.class);

            scene.run().landOnNext();

            assertTrue(heard.collect().stream().noneMatch(packet -> packet.soundEvent().equals(SoundEvent.BLOCK_NOTE_BLOCK_PLING)), "ascent jumps do not score");
        }
    }
}
