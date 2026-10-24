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

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Leaving, dying or changing instance ends a carry without an impulse. */
@ExtendWith(MicrotusExtension.class)
class ThrowModuleLifecycleTest {

    @DisplayName("The thrower leaving the game ends the carry without an impulse")
    @Test
    void throwerDisconnectEndsTheCarryWithoutImpulse(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().grant(ThrowModule.PERMISSION), new FakePlayerSettings(), ThrowFixtures.clock(), Telemetry.noop());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                env.process().eventHandler().call(new PlayerDisconnectEvent(thrower));

                Assertions.assertEquals(0.0, target.getVelocity().length(), 1e-6, "a thrower who leaves must not launch the target");
                Assertions.assertFalse(ThrowFixtures.carries(thrower, target), "the target must be released when the thrower leaves");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The thrower dying ends the carry without an impulse")
    @Test
    void throwerDeathEndsTheCarryWithoutImpulse(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().grant(ThrowModule.PERMISSION), new FakePlayerSettings(), ThrowFixtures.clock(), Telemetry.noop());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                env.process().eventHandler().call(new PlayerDeathEvent(thrower, Component.empty(), Component.empty()));

                Assertions.assertEquals(0.0, target.getVelocity().length(), 1e-6, "a dying thrower must not launch the target");
                Assertions.assertFalse(ThrowFixtures.carries(thrower, target), "the target must be released when the thrower dies");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The thrower changing instance ends the carry without an impulse")
    @Test
    void instanceChangeEndsTheCarryWithoutImpulse(Env env) {
        Instance instance = env.createFlatInstance();
        Instance other = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().grant(ThrowModule.PERMISSION), new FakePlayerSettings(), ThrowFixtures.clock(), Telemetry.noop());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                thrower.setInstance(other, ThrowFixtures.SPOT).join();

                Assertions.assertEquals(0.0, target.getVelocity().length(), 1e-6, "an instance change must not launch the target");
                Assertions.assertFalse(ThrowFixtures.carries(thrower, target), "the target must be released when the thrower changes instance");
            } finally {
                module.stop();
            }
        }
    }
}
