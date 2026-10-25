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
import net.onelitefeather.titan.core.testfixtures.AdjustableClock;
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The carrier sees the carried player's body in front of the camera. The carried player is hidden
 * from the carrier alone while the carry runs, and every way a carry ends shows them again.
 */
@ExtendWith(MicrotusExtension.class)
class ThrowCarrierViewTest {

    @DisplayName("The carried player is hidden from the carrier during the carry")
    @Test
    void carriedPlayerIsHiddenFromTheCarrier(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry testTelemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().allowEveryone(ThrowModule.PERMISSION), new FakePlayerSettings(), ThrowFixtures.clock(), testTelemetry.telemetry());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);

                Assertions.assertTrue(ThrowFixtures.carries(thrower, target), "the target must be carried for this test to mean anything");
                Assertions.assertFalse(target.isViewer(thrower), "the carrier must not see the carried player's body in front of the camera");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Other players still see the carried player")
    @Test
    void carriedPlayerStaysVisibleToOthers(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player bystander = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry testTelemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().allowEveryone(ThrowModule.PERMISSION), new FakePlayerSettings(), ThrowFixtures.clock(), testTelemetry.telemetry());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);

                Assertions.assertTrue(target.isViewer(bystander), "the hiding must apply to the carrier only");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The carried player is visible to the carrier again after a throw")
    @Test
    void throwShowsTheCarriedPlayerAgain(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        AdjustableClock clock = ThrowFixtures.clock();

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry testTelemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().allowEveryone(ThrowModule.PERMISSION), new FakePlayerSettings(), clock, testTelemetry.telemetry());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                clock.advance(Duration.ofMillis(600));
                ThrowFixtures.beginCharge(env, thrower);
                ThrowFixtures.release(env, thrower, ThrowFixtures.FULL_CHARGE);

                Assertions.assertFalse(ThrowFixtures.carries(thrower, target), "the throw must end the carry");
                Assertions.assertTrue(target.isViewer(thrower), "after the throw the carrier must see the thrown player again");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The carried player is visible to the carrier again after the target escaped")
    @Test
    void escapeShowsTheCarriedPlayerAgain(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry testTelemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().allowEveryone(ThrowModule.PERMISSION), new FakePlayerSettings(), ThrowFixtures.clock(), testTelemetry.telemetry());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                ThrowFixtures.sneak(env, target);

                Assertions.assertFalse(ThrowFixtures.carries(thrower, target), "sneaking must end the carry");
                Assertions.assertTrue(target.isViewer(thrower), "after the escape the carrier must see the player again");
            } finally {
                module.stop();
            }
        }
    }
}
