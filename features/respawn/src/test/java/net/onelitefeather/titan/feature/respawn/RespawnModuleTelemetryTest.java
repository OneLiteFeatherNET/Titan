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
package net.onelitefeather.titan.feature.respawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.common.Attributes;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.item.LobbyItems;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * Telemetry of {@link RespawnModule}: the deferred respawn is a span carrying the player, and the
 * respawn counter. Each test builds its own telemetry and node and drives ticks explicitly.
 */
@ExtendWith(MicrotusExtension.class)
class RespawnModuleTelemetryTest {

    @DisplayName("A respawn opens a respawn.perform span carrying the player's UUID")
    @Test
    void aRespawnOpensARespawnPerformSpanWithThePlayer(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        try (TestTelemetry telemetry = TestTelemetry.create(); TestTitanNode titan = TestTitanNode.attach(env)) {
            RespawnModule module = startedModule(titan, telemetry);
            try {
                player.kill();
                env.tick();

                assertEquals(player.getUuid().toString(), telemetry.attribute(telemetry.span("respawn.perform"), Telemetry.USER_ID), "the player's UUID");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A respawn counts one player respawn")
    @Test
    void aRespawnCountsOnePlayerRespawn(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        try (TestTelemetry telemetry = TestTelemetry.create(); TestTitanNode titan = TestTitanNode.attach(env)) {
            RespawnModule module = startedModule(titan, telemetry);
            try {
                player.kill();
                env.tick();

                assertEquals(1, telemetry.counter("player.respawns", Attributes.empty()), "one respawn");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A disconnect before the next tick opens no respawn span")
    @Test
    void aDisconnectBeforeTheTickOpensNoRespawnSpan(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        try (TestTelemetry telemetry = TestTelemetry.create(); TestTitanNode titan = TestTitanNode.attach(env)) {
            RespawnModule module = startedModule(titan, telemetry);
            try {
                player.kill();
                player.remove();
                env.tick();

                assertTrue(telemetry.spans().stream().noneMatch(span -> span.getName().equals("respawn.perform")), "the deferred respawn died with the player's scheduler");
            } finally {
                module.stop();
            }
        }
    }

    private static RespawnModule startedModule(TestTitanNode titan, TestTelemetry telemetry) {
        RespawnModule module = new RespawnModule(titan.node(), Mockito.mock(LobbyItems.class), telemetry.telemetry());
        module.start();
        return module;
    }
}
