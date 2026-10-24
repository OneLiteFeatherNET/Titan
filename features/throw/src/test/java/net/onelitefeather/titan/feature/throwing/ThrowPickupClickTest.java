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

import io.opentelemetry.api.common.Attributes;
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
 * What one right-click does: the client sends the interact more than once per click, and holding
 * the button repeats it. Only one pickup may come of a click, and the feedback stays quiet.
 */
@ExtendWith(MicrotusExtension.class)
class ThrowPickupClickTest {

    /** Minestom ticks are 50 ms; the clock moves with them. */
    private static final Duration TICK = Duration.ofMillis(50);

    @DisplayName("Two main-hand interacts of one click pick the target up once")
    @Test
    void oneClickPicksUpOnce(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        ThrowFixtures.ActionBars actionBars = new ThrowFixtures.ActionBars();

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry testTelemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().allowEveryone(ThrowModule.PERMISSION), new FakePlayerSettings(), ThrowFixtures.clock(), testTelemetry.telemetry(), actionBars);
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                ThrowFixtures.rightClick(env, thrower, target);

                Assertions.assertTrue(ThrowFixtures.carries(thrower, target), "the first interact of the click must pick the target up");
                Assertions.assertEquals(1, testTelemetry.counter("titan.throw.pickups", Attributes.empty()), "the second interact of the same click must not start a second pickup");
                Assertions.assertEquals(0, actionBars.countRefusalsFor(thrower), "a second interact of the same click must not show a refusal");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A held right-click after the target escaped shows at most one refusal per second")
    @Test
    void heldClickShowsAtMostOneRefusalPerSecond(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        AdjustableClock clock = ThrowFixtures.clock();
        ThrowFixtures.ActionBars actionBars = new ThrowFixtures.ActionBars();

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry testTelemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().allowEveryone(ThrowModule.PERMISSION), new FakePlayerSettings(), clock, testTelemetry.telemetry(), actionBars);
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                clock.advance(Duration.ofMillis(600));
                ThrowFixtures.sneak(env, target);
                for (int tick = 0; tick < 40; tick++) {
                    if (tick % 4 == 0) {
                        ThrowFixtures.rightClick(env, thrower, target);
                    }
                    clock.advance(TICK);
                    env.tick();
                }

                Assertions.assertFalse(ThrowFixtures.carries(thrower, target), "the target escaped, so no carry runs");
                Assertions.assertTrue(actionBars.countRefusalsFor(thrower) >= 1, "the refused held click must be explained once");
                Assertions.assertTrue(actionBars.countRefusalsFor(thrower) <= 2, "two seconds of holding must show at most two refusals, showed " + actionBars.countRefusalsFor(thrower));
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A player without the throw permission gets no text for a right-click")
    @Test
    void permissionlessClickIsSilent(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        ThrowFixtures.ActionBars actionBars = new ThrowFixtures.ActionBars();

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry testTelemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService(), new FakePlayerSettings(), ThrowFixtures.clock(), testTelemetry.telemetry(), actionBars);
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);

                Assertions.assertFalse(ThrowFixtures.carries(thrower, target), "without the permission nobody is picked up");
                Assertions.assertEquals(0, actionBars.countRefusalsFor(thrower), "a player without the permission must not be told about a feature that is not theirs");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A refusal is shown to the thrower only")
    @Test
    void refusalGoesToTheThrowerOnly(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        FakePlayerSettings settings = new FakePlayerSettings();
        settings.set(target.getUuid(), ThrowSettings.TARGET, false);
        ThrowFixtures.ActionBars actionBars = new ThrowFixtures.ActionBars();

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry testTelemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().allowEveryone(ThrowModule.PERMISSION), settings, ThrowFixtures.clock(), testTelemetry.telemetry(), actionBars);
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);

                Assertions.assertEquals(1, actionBars.countRefusalsFor(thrower), "the thrower must be told why the pickup failed, once");
                Assertions.assertEquals(0, actionBars.countFor(target), "the target must not be told about a refused pickup");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A refused pickup does not start the target's cooldown")
    @Test
    void refusedPickupStartsNoCooldown(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        FakePlayerSettings settings = new FakePlayerSettings();
        settings.set(target.getUuid(), ThrowSettings.TARGET, false);
        AdjustableClock clock = ThrowFixtures.clock();

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry testTelemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().allowEveryone(ThrowModule.PERMISSION), settings, clock, testTelemetry.telemetry());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                settings.set(target.getUuid(), ThrowSettings.TARGET, true);
                clock.advance(Duration.ofMillis(600));
                ThrowFixtures.rightClick(env, thrower, target);

                Assertions.assertTrue(ThrowFixtures.carries(thrower, target), "a refused attempt must leave no cooldown behind, so the next click picks the target up");
            } finally {
                module.stop();
            }
        }
    }
}
