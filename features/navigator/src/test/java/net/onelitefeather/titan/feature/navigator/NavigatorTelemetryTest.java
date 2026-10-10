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
package net.onelitefeather.titan.feature.navigator;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.SpanData;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tracing of the navigator on a real {@code Env}: opening spans the menu's kind and entry count,
 * each click spans the destination and its outcome, and the selection counter follows.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorTelemetryTest {

    private static final String BUILD_PERMISSION = "titan.navigator.buildserver";
    private static final int SURVIVAL_SLOT = 4;
    private static final int BUILD_SLOT = 7;
    private static final int SPAWN_SLOT = 2;

    private static final AttributeKey<String> KIND = AttributeKey.stringKey("navigator.kind");
    private static final AttributeKey<Long> ENTRIES = AttributeKey.longKey("navigator.entries");
    private static final AttributeKey<String> DESTINATION = AttributeKey.stringKey("navigator.destination");
    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("navigator.result");
    private static final AttributeKey<String> METRIC_DESTINATION = AttributeKey.stringKey("destination");
    private static final AttributeKey<String> METRIC_RESULT = AttributeKey.stringKey("result");

    private final TestTelemetry testTelemetry = TestTelemetry.create();

    @AfterEach
    void closeTelemetry() {
        testTelemetry.close();
    }

    private static FakeFeatureFlags slenderActive() {
        return new FakeFeatureFlags().declare("NAVIGATOR_SLENDER", true);
    }

    private NavigatorFixture start(Env env, RecordingDeliver deliver, FakePermissionService permissions) {
        return NavigatorFixture.start(env, deliver, slenderActive(), permissions, new FakeSpawnReturn(), testTelemetry.telemetry());
    }

    private static void click(Env env, Player player, AbstractInventory inventory, int slot) {
        env.process().eventHandler().call(new InventoryPreClickEvent(inventory, player, new Click.Left(slot)));
    }

    @DisplayName("Opening the public navigator spans its kind and its entries, the spawn entry included")
    @Test
    void openingThePublicNavigatorSpansKindAndEntries(Env env) {
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), new FakePermissionService())) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            fixture.equip(player);

            fixture.useFeather(player);

            SpanData span = testTelemetry.span("navigator.open");
            Assertions.assertEquals("public", testTelemetry.attribute(span, KIND), "a player without the build permission gets the public menu");
            Assertions.assertEquals(player.getUuid().toString(), testTelemetry.attribute(span, Telemetry.USER_ID), "the player UUID is the user.id");
            Assertions.assertEquals(5L, testTelemetry.attribute(span, ENTRIES), "four public destinations plus the spawn entry");
        }
    }

    @DisplayName("Opening the navigator as a permitted player spans the team kind with the permissioned destination")
    @Test
    void openingAsAPermittedPlayerSpansTheTeamKind(Env env) {
        FakePermissionService permissions = new FakePermissionService();
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), permissions)) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            permissions.set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
            fixture.equip(player);

            fixture.useFeather(player);

            SpanData span = testTelemetry.span("navigator.open");
            Assertions.assertEquals("team", testTelemetry.attribute(span, KIND), "a permitted player gets the team menu");
            Assertions.assertEquals(player.getUuid().toString(), testTelemetry.attribute(span, Telemetry.USER_ID), "the player UUID is the user.id");
            Assertions.assertEquals(6L, testTelemetry.attribute(span, ENTRIES), "five destinations including Build, plus the spawn entry");
        }
    }

    @DisplayName("Selecting an allowed destination spans sent and counts it by destination and result")
    @Test
    void selectingAnAllowedDestinationSpansSent(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        try (NavigatorFixture fixture = start(env, deliver, new FakePermissionService())) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            fixture.equip(player);
            fixture.useFeather(player);

            click(env, player, player.getOpenInventory(), SURVIVAL_SLOT);

            SpanData span = testTelemetry.span("navigator.select");
            Assertions.assertEquals("SURVIVAL", testTelemetry.attribute(span, DESTINATION), "the destination's name from the fixed enumeration");
            Assertions.assertEquals("sent", testTelemetry.attribute(span, RESULT), "an allowed destination is sent");
            Assertions.assertEquals(player.getUuid().toString(), testTelemetry.attribute(span, Telemetry.USER_ID), "the player UUID is the user.id");
            Assertions.assertEquals(1, testTelemetry.counter("titan.navigator.selections", Attributes.of(METRIC_DESTINATION, "SURVIVAL", METRIC_RESULT, "sent")), "the selection is counted");
        }
    }

    @DisplayName("Selecting a destination the player may no longer use spans denied and sends nobody")
    @Test
    void selectingADeniedDestinationSpansDenied(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePermissionService permissions = new FakePermissionService();
        try (NavigatorFixture fixture = start(env, deliver, permissions)) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            permissions.set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
            fixture.equip(player);
            fixture.useFeather(player);
            permissions.set(player.getUuid(), BUILD_PERMISSION, PermissionResult.DENIED);

            click(env, player, player.getOpenInventory(), BUILD_SLOT);

            SpanData span = testTelemetry.span("navigator.select");
            Assertions.assertEquals("denied", testTelemetry.attribute(span, RESULT), "the revoked destination is denied");
            Assertions.assertEquals(player.getUuid().toString(), testTelemetry.attribute(span, Telemetry.USER_ID), "the player UUID is the user.id");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a denied destination must not transfer the player");
            Assertions.assertEquals(1, testTelemetry.counter("titan.navigator.selections", Attributes.of(METRIC_DESTINATION, "BUILD", METRIC_RESULT, "denied")), "the denial is counted");
        }
    }

    @DisplayName("Selecting the spawn entry spans spawn with its own destination value")
    @Test
    void selectingTheSpawnEntrySpansSpawn(Env env) {
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), new FakePermissionService())) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            fixture.equip(player);
            fixture.useFeather(player);

            click(env, player, player.getOpenInventory(), SPAWN_SLOT);

            SpanData span = testTelemetry.span("navigator.select");
            Assertions.assertEquals("spawn", testTelemetry.attribute(span, DESTINATION), "the spawn entry is not a Destination, it has its own value");
            Assertions.assertEquals("spawn", testTelemetry.attribute(span, RESULT), "the spawn entry reports spawn");
            Assertions.assertEquals(player.getUuid().toString(), testTelemetry.attribute(span, Telemetry.USER_ID), "the player UUID is the user.id");
            Assertions.assertEquals(1, testTelemetry.counter("titan.navigator.selections", Attributes.of(METRIC_DESTINATION, "spawn", METRIC_RESULT, "spawn")), "the spawn selection is counted");
        }
    }
}
