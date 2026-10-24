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
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class ThrowFlightGuardIntegrationTest {

    @DisplayName("A thrown player may not be handed to another server while in the air")
    @Test
    void thrownPlayerStaysOnThisServerDuringFlight(Env env) {
        Instance instance = env.createFlatInstance();
        Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);

        AdjustableClock clock = ThrowFixtures.clock();

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = ThrowFixtures.module(env, titan, new FakePermissionService().allowEveryone(ThrowModule.PERMISSION), new FakePlayerSettings(), clock, Telemetry.noop());
            module.start();
            try {
                ThrowFixtures.rightClick(env, thrower, target);
                ThrowFixtures.beginChargeAfterGrace(env, thrower, clock);
                ThrowFixtures.release(env, thrower, ThrowFixtures.FULL_CHARGE);

                Assertions.assertFalse(module.transferGuard().mayTransfer(target.getUuid()), "a player in the air must not be handed over to another server");
                Assertions.assertTrue(module.transferGuard().mayTransfer(thrower.getUuid()), "the thrower was not thrown and may pass a portal");
            } finally {
                module.stop();
            }
        }
    }
}
