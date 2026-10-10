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
package net.onelitefeather.titan.platform.luckperms;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The permission telemetry without a LuckPerms server: the check is handed in as its result, so
 * the counter, the span event and the start span are exercised on their own.
 */
class LuckPermsTelemetryTest {

    private static final String STOP = "titan.command.stop";
    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("result");

    private final TestTelemetry testTelemetry = TestTelemetry.create();
    private final LuckPermsTelemetry telemetry = new LuckPermsTelemetry(testTelemetry.telemetry());

    @AfterEach
    void close() {
        testTelemetry.close();
    }

    @DisplayName("A denied check increments the counter for the denied result")
    @Test
    void aDeniedCheckIncrementsTheDeniedCounter() {
        telemetry.checked(STOP, PermissionResult.DENIED);

        Assertions.assertEquals(1, testTelemetry.counter("permission.checks", Attributes.of(RESULT, "denied")), "the denied counter");
    }

    @DisplayName("A check without a running span is counted and opens no span")
    @Test
    void aCheckWithoutASpanIsCountedAndOpensNoSpan() {
        PermissionResult result = telemetry.checked(STOP, PermissionResult.ALLOWED);

        Assertions.assertEquals(PermissionResult.ALLOWED, result, "the outcome is handed back unchanged");
        Assertions.assertEquals(1, testTelemetry.counter("permission.checks", Attributes.of(RESULT, "allowed")), "the allowed counter");
        Assertions.assertTrue(testTelemetry.spans().isEmpty(), "a check must not open a span of its own");
    }

    @DisplayName("A check inside a running span adds a permission.check event with its permission and result")
    @Test
    void aCheckInsideASpanAddsAPermissionCheckEvent() {
        testTelemetry.telemetry().inSpan("portal.transfer", Attributes.empty(), () -> {
            telemetry.checked(STOP, PermissionResult.DENIED);
        });

        SpanData portal = testTelemetry.span("portal.transfer");
        List<EventData> checks = portal.getEvents().stream().filter(event -> event.getName().equals("permission.check")).toList();
        Assertions.assertEquals(1, checks.size(), "exactly one permission.check event on the running span");
        Assertions.assertEquals(STOP, checks.getFirst().getAttributes().get(AttributeKey.stringKey("permission")), "the event names the permission");
        Assertions.assertEquals("denied", checks.getFirst().getAttributes().get(RESULT), "the event carries the result");
    }

    @DisplayName("The platform start span names luckperms and reports that no extension was loaded")
    @Test
    void theStartSpanNamesThePlatform() {
        AtomicBoolean started = new AtomicBoolean();

        telemetry.started(List.of("SomeOtherExtension"), () -> started.set(true));

        SpanData span = testTelemetry.span("permission.platform.start");
        Assertions.assertEquals("luckperms", span.getAttributes().get(AttributeKey.stringKey("permission.platform")), "the platform name");
        Assertions.assertEquals(false, span.getAttributes().get(AttributeKey.booleanKey("luckperms.extension.loaded")), "no LuckPerms extension was loaded");
        Assertions.assertTrue(started.get(), "the platform start must run");
    }

    @DisplayName("A loaded LuckPerms extension fails the start span and the exception reaches the caller")
    @Test
    void aLoadedLuckPermsExtensionFailsTheStartSpan() {
        AtomicBoolean started = new AtomicBoolean();

        Assertions.assertThrows(IllegalStateException.class, () -> telemetry.started(List.of("LuckPerms"), () -> started.set(true)), "the duplicate load must abort the start");

        SpanData span = testTelemetry.span("permission.platform.start");
        Assertions.assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode(), "the start span is marked as failed");
        Assertions.assertEquals(true, span.getAttributes().get(AttributeKey.booleanKey("luckperms.extension.loaded")), "the span records the loaded extension");
        Assertions.assertFalse(started.get(), "the platform must not start after the duplicate load");
    }
}
