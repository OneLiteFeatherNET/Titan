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

import java.time.Duration;
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
class ThrowModuleChargeTest {

    @DisplayName("A permitted right-click on a player puts that player on the thrower's head")
    @Test
    void pickupPutsTheTargetOnTheHead(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().grant(ThrowModule.PERMISSION), new FakePlayerSettings(), ThrowFixtures.clock(), Telemetry.noop());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);

                Assertions.assertTrue(ThrowFixtures.carries(thrower, target), "the target must ride on the thrower after a permitted pickup");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A pickup puts the charge item into the thrower's hand")
    @Test
    void pickupPutsTheChargeItemInHand(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().grant(ThrowModule.PERMISSION), new FakePlayerSettings(), ThrowFixtures.clock(), Telemetry.noop());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);

                Assertions.assertTrue(CarrierItem.isCarrier(ThrowFixtures.heldItem(thrower)), "the held item must be the charge item during a carry");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Holding the charge and releasing throws the target at the full speed of sixteen blocks per second")
    @Test
    void fullChargeThrowsAtSixteenBlocksPerSecond(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        AdjustableClock clock = ThrowFixtures.clock();

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().grant(ThrowModule.PERMISSION), new FakePlayerSettings(), clock, Telemetry.noop());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                ThrowFixtures.beginChargeAfterGrace(env, thrower, clock);
                ThrowFixtures.release(env, thrower, ThrowFixtures.FULL_CHARGE);

                Assertions.assertFalse(ThrowFixtures.carries(thrower, target), "a release must end the carry");
                Assertions.assertEquals(16.0, target.getVelocity().length(), 1e-6, "a full charge must throw at 16 blocks per second");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A tap throws at the minimum speed of eight blocks per second")
    @Test
    void tapThrowsAtEightBlocksPerSecond(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        AdjustableClock clock = ThrowFixtures.clock();

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().grant(ThrowModule.PERMISSION), new FakePlayerSettings(), clock, Telemetry.noop());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                ThrowFixtures.beginChargeAfterGrace(env, thrower, clock);
                ThrowFixtures.release(env, thrower, 1);

                Assertions.assertEquals(8.0, target.getVelocity().length(), 1e-6, "a tap of one tick must throw at 8 blocks per second");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Releasing without a running charge throws nothing and keeps the carry")
    @Test
    void releaseWithoutChargeKeepsTheCarry(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().grant(ThrowModule.PERMISSION), new FakePlayerSettings(), ThrowFixtures.clock(), Telemetry.noop());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                ThrowFixtures.release(env, thrower, ThrowFixtures.FULL_CHARGE);

                Assertions.assertTrue(ThrowFixtures.carries(thrower, target), "a release without a charge must not throw");
                Assertions.assertEquals(0.0, target.getVelocity().length(), 1e-6, "no impulse may be set without a charge");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A use click within half a second of the pickup is the same click and does not start a charge")
    @Test
    void clickRightAfterPickupDoesNotCharge(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        AdjustableClock clock = ThrowFixtures.clock();

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().grant(ThrowModule.PERMISSION), new FakePlayerSettings(), clock, Telemetry.noop());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                clock.advance(Duration.ofMillis(100));
                ThrowFixtures.beginCharge(env, thrower);
                ThrowFixtures.release(env, thrower, ThrowFixtures.FULL_CHARGE);

                Assertions.assertTrue(ThrowFixtures.carries(thrower, target), "a click within 500 ms of the pickup must not throw");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A use click half a second after the pickup starts a charge")
    @Test
    void clickAfterGraceStartsCharge(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        AdjustableClock clock = ThrowFixtures.clock();

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().grant(ThrowModule.PERMISSION), new FakePlayerSettings(), clock, Telemetry.noop());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                clock.advance(Duration.ofMillis(600));
                ThrowFixtures.beginCharge(env, thrower);
                ThrowFixtures.release(env, thrower, ThrowFixtures.FULL_CHARGE);

                Assertions.assertFalse(ThrowFixtures.carries(thrower, target), "a charge started after the grace period must throw");
            } finally {
                module.stop();
            }
        }
    }
}
