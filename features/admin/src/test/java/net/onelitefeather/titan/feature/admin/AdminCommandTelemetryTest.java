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
package net.onelitefeather.titan.feature.admin;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.util.TriState;
import net.minestom.server.command.CommandManager;
import net.minestom.server.entity.Player;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * Span and counter of {@code stop} and {@code end}, driven through a real {@link CommandManager}.
 * The shutdown and the thread launch are fakes, so no test stops anything or waits for a thread.
 */
@ExtendWith(MicrotusExtension.class)
class AdminCommandTelemetryTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-00000000ad31");
    private static final String SPAN = "admin.command";
    private static final AttributeKey<String> COMMAND = AttributeKey.stringKey("admin.command");
    private static final AttributeKey<String> SENDER = AttributeKey.stringKey("admin.sender");
    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("admin.result");

    private final TestTelemetry testTelemetry = TestTelemetry.create();
    private final AdminTelemetry telemetry = new AdminTelemetry(testTelemetry.telemetry());
    private final AtomicBoolean shutDown = new AtomicBoolean();

    @AfterEach
    void close() {
        testTelemetry.close();
    }

    private static Player playerWithStopPermission(TriState state) {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUuid()).thenReturn(PLAYER);
        Mockito.when(player.getOrDefault(Mockito.eq(PermissionChecker.POINTER), Mockito.any())).thenReturn(PermissionChecker.always(state));
        return player;
    }

    private StopCommand registerStop(Env env, Executor stopThread) {
        StopCommand stop = new StopCommand(telemetry, stopThread, () -> shutDown.set(true));
        env.process().command().register(stop);
        return stop;
    }

    private EndCommand registerEnd(Env env) {
        EndCommand end = new EndCommand(telemetry, () -> shutDown.set(true));
        env.process().command().register(end);
        return end;
    }

    private static Attributes counterAttributes(String command, String result) {
        return Attributes.of(AttributeKey.stringKey("command"), command, AttributeKey.stringKey("result"), result);
    }

    @DisplayName("A console stop is recorded as an executed span with the command and sender, and counted")
    @Test
    void aConsoleStopIsRecordedAsExecuted(Env env) {
        registerStop(env, Runnable::run);

        env.process().command().execute(env.process().command().getConsoleSender(), "stop");

        SpanData span = testTelemetry.span(SPAN);
        Assertions.assertEquals("stop", testTelemetry.attribute(span, COMMAND), "the span names the command");
        Assertions.assertEquals("console", testTelemetry.attribute(span, SENDER), "the span names the sender kind");
        Assertions.assertEquals("executed", testTelemetry.attribute(span, RESULT), "the run was executed");
        Assertions.assertNull(testTelemetry.attribute(span, Telemetry.USER_ID), "a console has no user.id");
        Assertions.assertEquals(1, testTelemetry.counter("admin.commands", counterAttributes("stop", "executed")), "the executed counter is incremented");
    }

    @DisplayName("A player with the stop permission carries his UUID on the executed span")
    @Test
    void aPlayerWithThePermissionCarriesHisUuid(Env env) {
        registerStop(env, Runnable::run);

        env.process().command().execute(playerWithStopPermission(TriState.TRUE), "stop");

        SpanData span = testTelemetry.span(SPAN);
        Assertions.assertEquals("player", testTelemetry.attribute(span, SENDER), "the span names a player sender");
        Assertions.assertEquals("executed", testTelemetry.attribute(span, RESULT), "the run was executed");
        Assertions.assertEquals(PLAYER.toString(), testTelemetry.attribute(span, Telemetry.USER_ID), "the span carries the player's UUID");
    }

    @DisplayName("A player without the stop permission is recorded as denied and does not stop the server")
    @Test
    void aPlayerWithoutThePermissionIsDeniedAndNotStopped(Env env) {
        registerStop(env, Runnable::run);

        env.process().command().execute(playerWithStopPermission(TriState.FALSE), "stop");

        SpanData span = testTelemetry.span(SPAN);
        Assertions.assertEquals("denied", testTelemetry.attribute(span, RESULT), "the refused run is denied");
        Assertions.assertEquals(PLAYER.toString(), testTelemetry.attribute(span, Telemetry.USER_ID), "the denied span carries the player's UUID");
        Assertions.assertEquals(1, testTelemetry.counter("admin.commands", counterAttributes("stop", "denied")), "the denied counter is incremented");
        Assertions.assertFalse(shutDown.get(), "a denied stop must not shut the server down");
    }

    @DisplayName("Building the command tree for a player without the permission records no denial")
    @Test
    void buildingTheCommandTreeRecordsNoDenial(Env env) {
        registerStop(env, Runnable::run);

        env.process().command().createDeclareCommandsPacket(playerWithStopPermission(TriState.FALSE));

        Assertions.assertTrue(testTelemetry.spans().isEmpty(), "building the tree is not a command attempt, so no span is recorded");
        Assertions.assertEquals(0, testTelemetry.counter("admin.commands", counterAttributes("stop", "denied")), "the denied counter stays at zero");
        Assertions.assertFalse(shutDown.get(), "building the tree must not shut the server down");
    }

    @DisplayName("A real denied stop attempt records exactly one denied span")
    @Test
    void aRealDeniedAttemptRecordsExactlyOneSpan(Env env) {
        registerStop(env, Runnable::run);

        env.process().command().execute(playerWithStopPermission(TriState.FALSE), "stop");

        Assertions.assertEquals(1, testTelemetry.spans().size(), "one command attempt, one span");
        Assertions.assertEquals(1, testTelemetry.counter("admin.commands", counterAttributes("stop", "denied")), "one attempt, one denial");
    }

    @DisplayName("The stop span has ended before the stop thread is handed the shutdown")
    @Test
    void theStopSpanEndsBeforeTheShutdownIsLaunched(Env env) {
        AtomicBoolean spanEndedBeforeLaunch = new AtomicBoolean();
        Executor stopThread = task -> {
            spanEndedBeforeLaunch.set(testTelemetry.spans().stream().anyMatch(span -> span.getName().equals(SPAN)));
            task.run();
        };
        registerStop(env, stopThread);

        env.process().command().execute(env.process().command().getConsoleSender(), "stop");

        Assertions.assertTrue(spanEndedBeforeLaunch.get(), "the admin.command span must end before the shutdown is launched");
        Assertions.assertTrue(shutDown.get(), "the launched shutdown must run");
    }

    @DisplayName("An end attempt is recorded as denied and never shuts the server down")
    @Test
    void anEndAttemptIsRecordedAsDenied(Env env) {
        registerEnd(env);

        env.process().command().execute(env.process().command().getConsoleSender(), "end");

        SpanData span = testTelemetry.span(SPAN);
        Assertions.assertEquals("end", testTelemetry.attribute(span, COMMAND), "the span names end");
        Assertions.assertEquals("denied", testTelemetry.attribute(span, RESULT), "end stays disabled, so it is denied");
        Assertions.assertEquals(1, testTelemetry.counter("admin.commands", counterAttributes("end", "denied")), "the denied counter is incremented");
        Assertions.assertFalse(shutDown.get(), "end must not shut the server down");
    }
}
