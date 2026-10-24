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
package net.onelitefeather.titan.feature.throwing;

import net.onelitefeather.titan.core.testfixtures.AdjustableClock;
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class ThrowPreviewRecipientTest {

    @DisplayName("The flight preview reaches the thrower and no one else")
    @Test
    void previewGoesOnlyToTheThrower(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection throwerConnection = env.createConnection();
        Player thrower = throwerConnection.connect(instance);
        thrower.teleport(ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        TestConnection bystanderConnection = env.createConnection();
        Player bystander = bystanderConnection.connect(instance);
        bystander.teleport(ThrowFixtures.SPOT);
        Collector<ParticlePacket> throwerParticles = throwerConnection.trackIncoming(ParticlePacket.class);
        Collector<ParticlePacket> bystanderParticles = bystanderConnection.trackIncoming(ParticlePacket.class);

        AdjustableClock clock = ThrowFixtures.clock();

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().allowEveryone(ThrowModule.PERMISSION), new FakePlayerSettings(), clock, Telemetry.noop());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                ThrowFixtures.beginChargeAfterGrace(env, thrower, clock);
                env.tick();

                Assertions.assertFalse(throwerParticles.collect().isEmpty(), "the thrower must see the predicted flight while charging");
                Assertions.assertTrue(bystanderParticles.collect().isEmpty(), "no other player may receive a preview packet");
            } finally {
                module.stop();
            }
        }
    }
}
