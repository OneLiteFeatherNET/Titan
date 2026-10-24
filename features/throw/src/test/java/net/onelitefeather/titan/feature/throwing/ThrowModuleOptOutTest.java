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

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class ThrowModuleOptOutTest {

    @DisplayName("A target who switched the option off cannot be picked up")
    @Test
    void optedOutTargetCannotBePickedUp(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        FakePlayerSettings settings = new FakePlayerSettings();
        settings.set(target.getUuid(), ThrowSettings.TARGET, false);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().grant(ThrowModule.PERMISSION), settings, ThrowFixtures.clock(), Telemetry.noop());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);

                Assertions.assertFalse(ThrowFixtures.carries(thrower, target), "a target with the opt-out must not be picked up");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Switching the option back on makes the next pickup possible")
    @Test
    void optingBackInAllowsThePickup(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        FakePlayerSettings settings = new FakePlayerSettings();
        settings.set(target.getUuid(), ThrowSettings.TARGET, false);
        settings.set(target.getUuid(), ThrowSettings.TARGET, true);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().grant(ThrowModule.PERMISSION), settings, ThrowFixtures.clock(), Telemetry.noop());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);

                Assertions.assertTrue(ThrowFixtures.carries(thrower, target), "the option is read on every pickup, so switching it back on must allow the pickup");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("An immune player cannot be picked up, even by a permitted thrower")
    @Test
    void immuneTargetCannotBePickedUp(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        FakePermissionService permissions = new FakePermissionService().grant(ThrowModule.PERMISSION);
        permissions.makeImmune(target.getUuid());

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = ThrowFixtures.module(env, titan, permissions, new FakePlayerSettings(), ThrowFixtures.clock(), Telemetry.noop());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);

                Assertions.assertFalse(ThrowFixtures.carries(thrower, target), "an immune target must not be picked up");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A thrower without the throw permission cannot pick anyone up")
    @Test
    void throwerWithoutPermissionCannotPickUp(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService(), new FakePlayerSettings(), ThrowFixtures.clock(), Telemetry.noop());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);

                Assertions.assertFalse(ThrowFixtures.carries(thrower, target), "without titan.throw no pickup may happen");
                Assertions.assertTrue(ThrowFixtures.heldItem(thrower).isAir(), "the hand must stay empty after a refused pickup");
            } finally {
                module.stop();
            }
        }
    }
}
