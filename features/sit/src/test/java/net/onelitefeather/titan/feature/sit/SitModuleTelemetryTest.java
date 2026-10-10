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
package net.onelitefeather.titan.feature.sit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import java.util.ArrayList;
import java.util.List;
import io.opentelemetry.sdk.trace.data.SpanData;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.event.EntityDismountEvent;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Telemetry of {@link SitModule}: a span per sit and per stand-up, with the reason it ended, and
 * the started and stopped session counter. Every test builds its own telemetry and node.
 */
@ExtendWith(MicrotusExtension.class)
class SitModuleTelemetryTest {

    private static final String SPRUCE_STAIRS = "minecraft:spruce_stairs";
    private static final AttributeKey<String> SIT_BLOCK = AttributeKey.stringKey("sit.block");
    private static final AttributeKey<String> STOP_REASON = AttributeKey.stringKey("sit.stop.reason");
    private static final AttributeKey<String> EVENT = AttributeKey.stringKey("event");

    @DisplayName("Sitting down opens a sit.start span carrying the block and the player's UUID")
    @Test
    void sittingDownOpensASitStartSpanWithTheBlockAndThePlayer(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (TestTelemetry telemetry = TestTelemetry.create(); TestTitanNode titan = TestTitanNode.attach(env)) {
            SitModule module = startedModule(titan, telemetry);
            try {
                sitOnSpruceStairs(env, player);

                SpanData span = telemetry.span("sit.start");
                assertEquals(SPRUCE_STAIRS, telemetry.attribute(span, SIT_BLOCK), "the block the player sat on");
                assertEquals(player.getUuid().toString(), telemetry.attribute(span, Telemetry.USER_ID), "the player's UUID, never the name");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Sitting down counts one started session")
    @Test
    void sittingDownCountsOneStartedSession(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (TestTelemetry telemetry = TestTelemetry.create(); TestTitanNode titan = TestTitanNode.attach(env)) {
            SitModule module = startedModule(titan, telemetry);
            try {
                sitOnSpruceStairs(env, player);

                assertEquals(1, telemetry.counter("titan.sit.sessions", Attributes.of(EVENT, "started")), "one started session");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Sneaking up opens a sit.stop span with the reason sneak and counts one stopped session")
    @Test
    void sneakingUpOpensASitStopSpanWithReasonSneak(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (TestTelemetry telemetry = TestTelemetry.create(); TestTitanNode titan = TestTitanNode.attach(env)) {
            SitModule module = startedModule(titan, telemetry);
            try {
                sitOnSpruceStairs(env, player);

                env.process().eventHandler().call(SitTestEvents.sneak(player));

                SpanData span = telemetry.span("sit.stop");
                assertEquals("sneak", telemetry.attribute(span, STOP_REASON), "the reason the seat was left");
                assertEquals(player.getUuid().toString(), telemetry.attribute(span, Telemetry.USER_ID), "the player's UUID");
                assertEquals(1, telemetry.counter("titan.sit.sessions", Attributes.of(EVENT, "stopped")), "one stopped session");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Sneaking while seated fires exactly one EntityDismountEvent and stops the sit with reason sneak")
    @Test
    void sneakingWhileSeatedFiresOneDismountEventAndStopsWithReasonSneak(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        List<EntityDismountEvent> dismounts = new ArrayList<>();
        env.process().eventHandler().addListener(EntityDismountEvent.class, dismounts::add);
        try (TestTelemetry telemetry = TestTelemetry.create(); TestTitanNode titan = TestTitanNode.attach(env)) {
            SitModule module = startedModule(titan, telemetry);
            try {
                sitOnSpruceStairs(env, player);

                env.process().eventHandler().call(SitTestEvents.sneak(player));

                assertEquals(1, dismounts.size(), "sneaking while seated must fire exactly one EntityDismountEvent");
                assertSame(player, dismounts.get(0).rider(), "the dismount event's rider must be the sneaking player");
                assertEquals("sneak", telemetry.attribute(telemetry.span("sit.stop"), STOP_REASON), "a sneak must stop the sit with reason sneak, not dismount");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A dismount opens a sit.stop span with the reason dismount")
    @Test
    void aDismountOpensASitStopSpanWithReasonDismount(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (TestTelemetry telemetry = TestTelemetry.create(); TestTitanNode titan = TestTitanNode.attach(env)) {
            SitModule module = startedModule(titan, telemetry);
            try {
                sitOnSpruceStairs(env, player);

                env.process().eventHandler().call(new EntityDismountEvent(player, player.getVehicle()));

                assertEquals("dismount", telemetry.attribute(telemetry.span("sit.stop"), STOP_REASON), "the reason the seat was left");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Disconnecting while sitting opens a sit.stop span with the reason disconnect")
    @Test
    void disconnectingWhileSittingOpensASitStopSpanWithReasonDisconnect(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (TestTelemetry telemetry = TestTelemetry.create(); TestTitanNode titan = TestTitanNode.attach(env)) {
            SitModule module = startedModule(titan, telemetry);
            try {
                sitOnSpruceStairs(env, player);

                env.process().eventHandler().call(SitTestEvents.disconnect(player));

                assertEquals("disconnect", telemetry.attribute(telemetry.span("sit.stop"), STOP_REASON), "the reason the seat was left");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Disconnecting without sitting opens no sit.stop span")
    @Test
    void disconnectingWithoutSittingOpensNoSitStopSpan(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (TestTelemetry telemetry = TestTelemetry.create(); TestTitanNode titan = TestTitanNode.attach(env)) {
            SitModule module = startedModule(titan, telemetry);
            try {
                env.process().eventHandler().call(SitTestEvents.disconnect(player));

                assertTrue(telemetry.spans().stream().noneMatch(span -> span.getName().equals("sit.stop")), "no seat was left, so no span");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A sneak packet while not sitting opens no span")
    @Test
    void aSneakPacketWhileNotSittingOpensNoSpan(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (TestTelemetry telemetry = TestTelemetry.create(); TestTitanNode titan = TestTitanNode.attach(env)) {
            SitModule module = startedModule(titan, telemetry);
            try {
                env.process().eventHandler().call(SitTestEvents.sneak(player));

                assertTrue(telemetry.spans().isEmpty(), "a sneak packet fires for every sneak, only a stand-up may open a span");
            } finally {
                module.stop();
            }
        }
    }

    private static SitModule startedModule(TestTitanNode titan, TestTelemetry telemetry) {
        SitModule module = new SitModule(titan.node(), telemetry.telemetry());
        module.start();
        return module;
    }

    private static void sitOnSpruceStairs(Env env, Player player) {
        Instance instance = player.getInstance();
        assertNotNull(instance, "the player must be in an instance");
        player.teleport(new Pos(0, 64, 0));
        env.process().eventHandler().call(SitTestEvents.clickBlock(player, instance, Block.fromKey(SPRUCE_STAIRS), new BlockVec(0, 64, 0)));
        assertNotNull(player.getVehicle(), "the player must be sitting before the test continues");
    }
}
