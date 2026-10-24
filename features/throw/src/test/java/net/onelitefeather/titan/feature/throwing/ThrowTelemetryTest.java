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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.client.play.ClientInputPacket;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class ThrowTelemetryTest {

    private static final AttributeKey<String> REASON = AttributeKey.stringKey("reason");

    @DisplayName("A pickup and a throw each count once")
    @Test
    void pickupAndThrowCountOnce(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);

        AdjustableClock clock = ThrowFixtures.clock();

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry telemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().allowEveryone(ThrowModule.PERMISSION), new FakePlayerSettings(), clock, telemetry.telemetry());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                ThrowFixtures.beginChargeAfterGrace(env, thrower, clock);
                ThrowFixtures.release(env, thrower, ThrowFixtures.FULL_CHARGE);

                Assertions.assertEquals(1, telemetry.counter("titan.throw.pickups", Attributes.empty()), "one pickup must be counted once");
                Assertions.assertEquals(1, telemetry.counter("titan.throw.throws", Attributes.empty()), "one throw must be counted once");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A refused pickup is counted with its reason")
    @Test
    void refusalCarriesItsReason(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry telemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService(), new FakePlayerSettings(), ThrowFixtures.clock(), telemetry.telemetry());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);

                Assertions.assertEquals(1, telemetry.counter("titan.throw.refusals", Attributes.of(REASON, "permission")), "the refusal must be counted under its reason");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A target sneaking out of the carry counts as an escape")
    @Test
    void sneakingCountsAsEscape(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry telemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().allowEveryone(ThrowModule.PERMISSION), new FakePlayerSettings(), ThrowFixtures.clock(), telemetry.telemetry());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                env.process().eventHandler().call(new PlayerPacketEvent(target, new ClientInputPacket(false, false, false, false, false, true, false)));

                Assertions.assertEquals(1, telemetry.counter("titan.throw.escapes", Attributes.empty()), "a sneak out of the carry must count as an escape");
            } finally {
                module.stop();
            }
        }
    }
}
