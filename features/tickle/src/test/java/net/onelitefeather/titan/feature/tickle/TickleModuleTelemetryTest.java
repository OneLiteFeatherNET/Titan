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
package net.onelitefeather.titan.feature.tickle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Telemetry of {@link TickleModule}: every attack is counted by its result, and none opens a span,
 * since a fight can produce many of them. Each test builds its own telemetry and node, and uses a
 * fixed clock so the cooldown never depends on when the test runs.
 */
@ExtendWith(MicrotusExtension.class)
class TickleModuleTelemetryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("result");

    @DisplayName("A tickle counts one attack with the result tickled")
    @Test
    void aTickleCountsOneTickledAttack(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection attackerConnection = env.createConnection();
        Player attacker = attackerConnection.connect(instance);
        Player target = env.createConnection().connect(instance);
        attacker.setItemInOffHand(ItemStack.of(Material.FEATHER));

        try (TestTelemetry telemetry = TestTelemetry.create(); TestTitanNode titanNode = TestTitanNode.attach(env)) {
            TickleModule module = new TickleModule(titanNode.node(), Clock.fixed(NOW, ZoneOffset.UTC), telemetry.telemetry());
            module.start();
            try {
                env.process().eventHandler().call(new EntityAttackEvent(attacker, target));

                assertEquals(1, telemetry.counter("tickle.attacks", Attributes.of(RESULT, "tickled")), "one tickle");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A hit within the cooldown counts one attack with the result cooldown")
    @Test
    void aHitWithinTheCooldownCountsACooldownAttack(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection attackerConnection = env.createConnection();
        Player attacker = attackerConnection.connect(instance);
        Player target = env.createConnection().connect(instance);
        attacker.setItemInOffHand(ItemStack.of(Material.FEATHER));

        try (TestTelemetry telemetry = TestTelemetry.create(); TestTitanNode titanNode = TestTitanNode.attach(env)) {
            TickleModule module = new TickleModule(titanNode.node(), Clock.fixed(NOW, ZoneOffset.UTC), telemetry.telemetry());
            module.start();
            try {
                env.process().eventHandler().call(new EntityAttackEvent(attacker, target));
                env.process().eventHandler().call(new EntityAttackEvent(attacker, target));

                assertEquals(1, telemetry.counter("tickle.attacks", Attributes.of(RESULT, "cooldown")), "one attack refused by the cooldown");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Attacks open no span, whether they tickle or hit the cooldown")
    @Test
    void attacksOpenNoSpan(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection attackerConnection = env.createConnection();
        Player attacker = attackerConnection.connect(instance);
        Player target = env.createConnection().connect(instance);
        attacker.setItemInOffHand(ItemStack.of(Material.FEATHER));

        try (TestTelemetry telemetry = TestTelemetry.create(); TestTitanNode titanNode = TestTitanNode.attach(env)) {
            TickleModule module = new TickleModule(titanNode.node(), Clock.fixed(NOW, ZoneOffset.UTC), telemetry.telemetry());
            module.start();
            try {
                env.process().eventHandler().call(new EntityAttackEvent(attacker, target));
                env.process().eventHandler().call(new EntityAttackEvent(attacker, target));

                assertTrue(telemetry.spans().isEmpty(), "a fight can produce many attacks, so they are counted, not traced");
            } finally {
                module.stop();
            }
        }
    }
}
