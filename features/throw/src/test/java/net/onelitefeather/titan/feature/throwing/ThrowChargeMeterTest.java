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
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent;
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
 * The experience bar is the charge meter: it fills while a charge runs, and the thrower's own
 * experience and level come back however the charge ends.
 */
@ExtendWith(MicrotusExtension.class)
class ThrowChargeMeterTest {

    private static final float EXPERIENCE = 0.25f;
    private static final int LEVEL = 3;
    private static final String HINT_START = "Hold right click";

    @DisplayName("The thrower's experience and level come back after a throw")
    @Test
    void experienceComesBackAfterAThrow(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        thrower.setExp(EXPERIENCE);
        thrower.setLevel(LEVEL);
        AdjustableClock clock = ThrowFixtures.clock();

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry testTelemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().allowEveryone(ThrowModule.PERMISSION), new FakePlayerSettings(), clock, testTelemetry.telemetry());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                clock.advance(Duration.ofMillis(600));
                ThrowFixtures.beginCharge(env, thrower);
                env.tick();
                Assertions.assertNotEquals(EXPERIENCE, thrower.getExp(), 1e-6, "while charging the experience bar must show the charge");

                ThrowFixtures.release(env, thrower, Charge.FULL_TICKS);

                Assertions.assertEquals(EXPERIENCE, thrower.getExp(), 1e-6, "the experience bar must be restored after the throw");
                Assertions.assertEquals(LEVEL, thrower.getLevel(), "the level must be restored after the throw");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The thrower's experience comes back when the charge is aborted by a slot change")
    @Test
    void experienceComesBackAfterAnAbort(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        thrower.setExp(EXPERIENCE);
        thrower.setLevel(LEVEL);
        AdjustableClock clock = ThrowFixtures.clock();
        int carrierSlot = thrower.getHeldSlot();

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry testTelemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().allowEveryone(ThrowModule.PERMISSION), new FakePlayerSettings(), clock, testTelemetry.telemetry());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                clock.advance(Duration.ofMillis(600));
                ThrowFixtures.beginCharge(env, thrower);
                env.tick();

                env.process().eventHandler().call(new PlayerChangeHeldSlotEvent(thrower, (byte) carrierSlot, (byte) ((carrierSlot + 1) % 9)));

                Assertions.assertEquals(EXPERIENCE, thrower.getExp(), 1e-6, "the experience bar must be restored when the charge is aborted");
                Assertions.assertEquals(LEVEL, thrower.getLevel(), "the level must be restored when the charge is aborted");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The thrower's experience comes back when the target escapes during a charge")
    @Test
    void experienceComesBackWhenTheTargetEscapes(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        thrower.setExp(EXPERIENCE);
        thrower.setLevel(LEVEL);
        AdjustableClock clock = ThrowFixtures.clock();

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry testTelemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().allowEveryone(ThrowModule.PERMISSION), new FakePlayerSettings(), clock, testTelemetry.telemetry());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                clock.advance(Duration.ofMillis(600));
                ThrowFixtures.beginCharge(env, thrower);
                env.tick();

                ThrowFixtures.sneak(env, target);

                Assertions.assertEquals(EXPERIENCE, thrower.getExp(), 1e-6, "the experience bar must be restored when the target escapes");
                Assertions.assertEquals(LEVEL, thrower.getLevel(), "the level must be restored when the target escapes");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The hint is shown once at the start of a carry and not again while charging")
    @Test
    void hintIsShownOnceAtTheStartOfACarry(Env env) {
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
                ThrowFixtures.beginCharge(env, thrower);
                for (int tick = 0; tick < 10; tick++) {
                    env.tick();
                }

                long hints = actionBars.textsFor(thrower).stream().filter(text -> text.contains(HINT_START)).count();
                Assertions.assertEquals(1, hints, "the hint must be shown once per carry, showed " + hints + " times");
            } finally {
                module.stop();
            }
        }
    }
}
