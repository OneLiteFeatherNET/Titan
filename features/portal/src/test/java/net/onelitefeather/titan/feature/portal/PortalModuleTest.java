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
package net.onelitefeather.titan.feature.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Disc;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Every test gets its own {@code Env}, titan node, module, deliver, permissions, clock and
 * telemetry.
 */
@ExtendWith(MicrotusExtension.class)
class PortalModuleTest {

    private static final Pos OUTSIDE = new Pos(10.5, 64, 0.5);
    private static final Pos INSIDE = new Pos(0.5, 64, 0.5);
    private static final Pos FAR = new Pos(10.5, 64, 5.5);
    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("result");
    private static final AttributeKey<String> PORTAL_RESULT = AttributeKey.stringKey("portal.result");

    private final AdjustableClock clock = new AdjustableClock(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
    private final RecordingDeliver deliver = new RecordingDeliver();
    private final FakePermissionService permissions = new FakePermissionService();
    private final TestTelemetry testTelemetry = TestTelemetry.create();

    @AfterEach
    void closeTelemetry() {
        this.testTelemetry.close();
    }

    private static Portal survivalBox() {
        return new Portal("survival", new Box(new Vec(0, 64, 0), new Vec(1, 65, 1)), "Survival", null);
    }

    private PortalModule start(Env env, TestTitanNode titan, Portal... portals) {
        PortalModule module = module(env, titan, this.deliver, portals);
        module.start();
        return module;
    }

    private PortalModule module(Env env, TestTitanNode titan, Deliver deliverToUse, Portal... portals) {
        return new PortalModule(titan.node(), () -> List.of(portals), deliverToUse, this.permissions, this.clock, env.createFlatInstance(), env.process().scheduler(), Runnable::run, portal -> new LabelReading.Local(0), new PortalSettings(1), this.testTelemetry.telemetry());
    }

    /** Fires the move event before the position changes, exactly as Minestom does. */
    private static void move(Env env, Player player, Pos to) {
        env.process().eventHandler().call(new PlayerMoveEvent(player, to, true));
        player.teleport(to).join();
        env.tick();
    }

    private Player playerAt(Env env, Pos position) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.teleport(position).join();
        return player;
    }

    @DisplayName("A player walking into a box is delivered to the portal's task exactly once")
    @Test
    void walkingIntoBoxDeliversOnce(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = start(env, titan, survivalBox());
            Player player = playerAt(env, OUTSIDE);

            move(env, player, INSIDE);

            Assertions.assertEquals(List.of(new RecordingDeliver.Delivery(player, "Survival")), this.deliver.deliveries(), "entering the box must deliver to 'Survival' once");
            module.stop();
        }
    }

    @DisplayName("An 8-block elytra step through a 1-block-thin disc is delivered")
    @Test
    void elytraStepThroughThinDiscDelivers(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = start(env, titan, new Portal("gate", new Disc(new Vec(0, 64, 0), 3, new Vec(1, 0, 0)), "Gate", null));
            Player player = playerAt(env, new Pos(-4, 64, 0));

            move(env, player, new Pos(4, 64, 0));

            Assertions.assertEquals(List.of(new RecordingDeliver.Delivery(player, "Gate")), this.deliver.deliveries(), "the move segment crosses the disc although neither position is near it");
            module.stop();
        }
    }

    @DisplayName("Staying in the box delivers no second time")
    @Test
    void stayingDoesNotDeliverAgain(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = start(env, titan, survivalBox());
            Player player = playerAt(env, OUTSIDE);
            move(env, player, INSIDE);
            this.clock.advance(Duration.ofSeconds(10));

            move(env, player, new Pos(1.2, 64, 0.5));

            Assertions.assertEquals(1, this.deliver.deliveries().size(), "moving within the box after the cooldown must not deliver again");
            module.stop();
        }
    }

    @DisplayName("Re-entering within the cooldown delivers nothing, after advancing the clock by 3 s it delivers")
    @Test
    void reenteringRespectsCooldown(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = start(env, titan, survivalBox());
            Player player = playerAt(env, OUTSIDE);
            move(env, player, INSIDE);
            move(env, player, OUTSIDE);
            move(env, player, FAR);

            move(env, player, INSIDE);
            Assertions.assertEquals(1, this.deliver.deliveries().size(), "re-entering right away is inside the cooldown");

            move(env, player, OUTSIDE);
            move(env, player, FAR);
            this.clock.advance(Duration.ofSeconds(3));
            move(env, player, INSIDE);

            Assertions.assertEquals(2, this.deliver.deliveries().size(), "re-entering 3 s after the delivery must deliver again");
            module.stop();
        }
    }

    @DisplayName("A player without the permission is not delivered and starts no cooldown")
    @Test
    void missingPermissionDeliversNothing(Env env) {
        for (PermissionResult missing : List.of(PermissionResult.NOT_SET, PermissionResult.DENIED)) {
            RecordingDeliver localDeliver = new RecordingDeliver();
            try (TestTitanNode titan = TestTitanNode.attach(env)) {
                Portal vip = new Portal("vip", new Box(new Vec(0, 64, 0), new Vec(1, 65, 1)), "Vip", "titan.portal.vip");
                Portal open = new Portal("survival", new Box(new Vec(20, 64, 0), new Vec(21, 65, 1)), "Survival", null);
                PortalModule module = module(env, titan, localDeliver, vip, open);
                module.start();
                Player player = playerAt(env, OUTSIDE);
                this.permissions.set(player.getUuid(), "titan.portal.vip", missing);

                move(env, player, INSIDE);
                move(env, player, new Pos(25.5, 64, 0.5));
                move(env, player, new Pos(20.5, 64, 0.5));

                Assertions.assertEquals(List.of(new RecordingDeliver.Delivery(player, "Survival")), localDeliver.deliveries(), "with " + missing + " only the open portal delivers, immediately after the rejected one");
                module.stop();
            }
        }
    }

    @DisplayName("A player with the permission is delivered")
    @Test
    void grantedPermissionDelivers(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = start(env, titan, new Portal("vip", new Box(new Vec(0, 64, 0), new Vec(1, 65, 1)), "Vip", "titan.portal.vip"));
            Player player = playerAt(env, OUTSIDE);
            this.permissions.set(player.getUuid(), "titan.portal.vip", PermissionResult.ALLOWED);

            move(env, player, INSIDE);

            Assertions.assertEquals(List.of(new RecordingDeliver.Delivery(player, "Vip")), this.deliver.deliveries(), "ALLOWED must deliver to 'Vip'");
            module.stop();
        }
    }

    @DisplayName("A world without portals delivers nobody")
    @Test
    void worldWithoutPortalsDeliversNothing(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = start(env, titan);
            Player player = playerAt(env, OUTSIDE);

            move(env, player, INSIDE);

            Assertions.assertTrue(this.deliver.deliveries().isEmpty(), "no portals, no delivery");
            module.stop();
        }
    }

    @DisplayName("A disconnect clears the player's state so a reconnect starts fresh")
    @Test
    void disconnectClearsState(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = start(env, titan, survivalBox());
            Player player = playerAt(env, OUTSIDE);
            move(env, player, INSIDE);
            move(env, player, OUTSIDE);

            env.process().eventHandler().call(new PlayerDisconnectEvent(player));
            move(env, player, INSIDE);

            Assertions.assertEquals(2, this.deliver.deliveries().size(), "the state of a disconnected player is gone, cooldown included");
            module.stop();
        }
    }

    @DisplayName("A delivered transfer is a portal.transfer span with portal, task, player and the delivered result")
    @Test
    void deliveredTransferIsASpan(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = start(env, titan, survivalBox());
            Player player = playerAt(env, OUTSIDE);

            move(env, player, INSIDE);

            SpanData span = this.testTelemetry.span("portal.transfer");
            assertEquals("survival", this.testTelemetry.attribute(span, AttributeKey.stringKey("portal.id")), "the portal id");
            assertEquals("Survival", this.testTelemetry.attribute(span, AttributeKey.stringKey("portal.task")), "the task");
            assertEquals(player.getUuid().toString(), this.testTelemetry.attribute(span, AttributeKey.stringKey("user.id")), "the player's uuid");
            assertEquals("delivered", this.testTelemetry.attribute(span, PORTAL_RESULT), "the result");
            assertEquals(StatusCode.UNSET, span.getStatus().getStatusCode(), "a delivered transfer is no error");
            module.stop();
        }
    }

    @DisplayName("Each delivered transfer counts once under its result")
    @Test
    void deliveredTransferIsCounted(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = start(env, titan, survivalBox());
            Player player = playerAt(env, OUTSIDE);

            move(env, player, INSIDE);

            assertEquals(1, this.testTelemetry.counter("titan.portal.transfers", Attributes.of(RESULT, "delivered")), "one delivered transfer");
            module.stop();
        }
    }

    @DisplayName("A transfer counter carries no player: the uuid is only a span attribute")
    @Test
    void transferCounterCarriesNoPlayer(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = start(env, titan, survivalBox());
            Player player = playerAt(env, OUTSIDE);

            move(env, player, INSIDE);

            Attributes withPlayer = Attributes.of(RESULT, "delivered", AttributeKey.stringKey("user.id"), player.getUuid().toString());
            assertEquals(0, this.testTelemetry.counter("titan.portal.transfers", withPlayer), "a counter point with the player's uuid must not exist");
            module.stop();
        }
    }

    @DisplayName("A failing delivery is an error span and an error count, and the exception still reaches the server")
    @Test
    void failingDeliveryIsMarkedAndRethrown(Env env) {
        List<Throwable> serverExceptions = new ArrayList<>();
        env.process().exception().setExceptionHandler(serverExceptions::add);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = module(env, titan, (player, component) -> {
                throw new IllegalStateException("message channel is closed");
            }, survivalBox());
            module.start();
            Player player = playerAt(env, OUTSIDE);

            move(env, player, INSIDE);

            SpanData span = this.testTelemetry.span("portal.transfer");
            assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode(), "the transfer span carries the error");
            assertEquals("error", this.testTelemetry.attribute(span, PORTAL_RESULT), "the result");
            assertEquals(1, this.testTelemetry.counter("titan.portal.transfers", Attributes.of(RESULT, "error")), "one failed transfer");
            assertTrue(serverExceptions.stream().anyMatch(failure -> failure instanceof IllegalStateException), "the exception goes on, it is not swallowed");
            module.stop();
        }
    }

    @DisplayName("A move through no portal creates no span")
    @Test
    void movingWithoutPortalCreatesNoSpan(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = start(env, titan, survivalBox());
            Player player = playerAt(env, OUTSIDE);

            move(env, player, FAR);

            assertTrue(this.testTelemetry.spans().isEmpty(), "a move that enters no portal is not traced");
            module.stop();
        }
    }

    @DisplayName("A refused entry counts as denied under its portal id and creates no transfer span")
    @Test
    void refusedEntryIsCountedNotTraced(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            Portal vip = new Portal("vip", new Box(new Vec(0, 64, 0), new Vec(1, 65, 1)), "Vip", "titan.portal.vip");
            PortalModule module = start(env, titan, vip);
            Player player = playerAt(env, OUTSIDE);
            this.permissions.set(player.getUuid(), "titan.portal.vip", PermissionResult.DENIED);

            move(env, player, INSIDE);

            assertEquals(1, this.testTelemetry.counter("titan.portal.denied", Attributes.of(AttributeKey.stringKey("portal.id"), "vip")), "one refusal for 'vip'");
            assertTrue(this.testTelemetry.spans().stream().noneMatch(span -> span.getName().equals("portal.transfer")), "a refused entry is no transfer");
            module.stop();
        }
    }
}
