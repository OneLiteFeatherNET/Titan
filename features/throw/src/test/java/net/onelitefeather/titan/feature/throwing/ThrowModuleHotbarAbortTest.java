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
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent;
import net.minestom.server.event.item.PlayerCancelItemUseEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import io.opentelemetry.api.common.Attributes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class ThrowModuleHotbarAbortTest {

    @DisplayName("Changing the hotbar slot during a charge aborts the charge and does not throw")
    @Test
    void slotChangeAbortsTheCharge(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        AdjustableClock clock = ThrowFixtures.clock();
        int carrierSlot = thrower.getHeldSlot();
        int otherSlot = (carrierSlot + 1) % 9;

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry testTelemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().grant(ThrowModule.PERMISSION), new FakePlayerSettings(), clock, testTelemetry.telemetry());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                clock.advance(Duration.ofMillis(600));
                ThrowFixtures.beginCharge(env, thrower);
                env.process().eventHandler().call(new PlayerChangeHeldSlotEvent(thrower, (byte) carrierSlot, (byte) otherSlot));
                env.process().eventHandler().call(new PlayerCancelItemUseEvent(thrower, PlayerHand.MAIN, CarrierItem.create(), ThrowFixtures.FULL_CHARGE));

                Assertions.assertTrue(ThrowFixtures.carries(thrower, target), "an aborted charge must not throw the target");
                Assertions.assertTrue(CarrierItem.isCarrier(thrower.getInventory().getItemStack(carrierSlot)), "the charge item must stay in its slot after the abort");
                Assertions.assertEquals(1, testTelemetry.counter("titan.throw.charge_aborts", Attributes.empty()), "the abort must be counted once");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Changing the hotbar slot while only carrying, without a charge, changes nothing")
    @Test
    void slotChangeWithoutChargeChangesNothing(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        int carrierSlot = thrower.getHeldSlot();

        try (TestTitanNode titan = TestTitanNode.attach(env); TestTelemetry testTelemetry = TestTelemetry.create()) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().grant(ThrowModule.PERMISSION), new FakePlayerSettings(), ThrowFixtures.clock(), testTelemetry.telemetry());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                env.process().eventHandler().call(new PlayerChangeHeldSlotEvent(thrower, (byte) carrierSlot, (byte) ((carrierSlot + 1) % 9)));

                Assertions.assertEquals(0, testTelemetry.counter("titan.throw.charge_aborts", Attributes.empty()), "only a running charge counts as aborted");
                Assertions.assertTrue(ThrowFixtures.carries(thrower, target), "the carry must survive a slot change without a charge");
            } finally {
                module.stop();
            }
        }
    }
}
