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
package net.onelitefeather.titan.core.telemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TelemetryTest {

    private static final AttributeKey<String> KEY = AttributeKey.stringKey("test.key");

    @DisplayName("inSpan records one span with the given attributes and returns the body's value")
    @Test
    void inSpanRecordsASpanWithAttributes() {
        try (TestTelemetry test = TestTelemetry.create()) {
            String result = test.telemetry().inSpan("test.op", Attributes.of(KEY, "value"), () -> "done");

            Assertions.assertEquals("done", result, "the body's value must be returned");
            SpanData span = test.span("test.op");
            Assertions.assertEquals("value", test.attribute(span, KEY));
            Assertions.assertEquals(StatusCode.UNSET, span.getStatus().getStatusCode(), "a successful span keeps the default status");
        }
    }

    @DisplayName("The span is current while the body runs and no longer afterwards")
    @Test
    void spanIsCurrentOnlyWhileTheBodyRuns() {
        try (TestTelemetry test = TestTelemetry.create()) {
            String[] insideSpanId = new String[1];

            test.telemetry().inSpan("test.op", Attributes.empty(), () -> insideSpanId[0] = Span.current().getSpanContext().getSpanId());

            Assertions.assertEquals(test.span("test.op").getSpanId(), insideSpanId[0], "the body must see the span as current");
            Assertions.assertFalse(Span.current().getSpanContext().isValid(), "the span must not stay current after the body");
        }
    }

    @DisplayName("An exception is recorded with status ERROR, rethrown, and the span still ends")
    @Test
    void inSpanRecordsAndRethrowsAnException() {
        try (TestTelemetry test = TestTelemetry.create()) {
            IllegalStateException failure = new IllegalStateException("boom");

            IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> test.telemetry().inSpan("test.op", Attributes.empty(), () -> {
                throw failure;
            }));

            Assertions.assertSame(failure, thrown, "the exception must not be wrapped");
            SpanData span = test.span("test.op");
            Assertions.assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
            Assertions.assertEquals("exception", span.getEvents().getFirst().getName(), "the exception must be recorded as a span event");
        }
    }

    @DisplayName("noop() runs the body and records nothing")
    @Test
    void noopRunsTheBodyWithoutRecording() {
        Telemetry noop = Telemetry.noop();

        String result = noop.inSpan("test.op", Attributes.empty(), () -> "done");

        Assertions.assertEquals("done", result);
        Assertions.assertFalse(noop.tracer().spanBuilder("x").startSpan().getSpanContext().isValid(), "a no-op span has no valid context");
    }

    @DisplayName("noop() still rethrows the body's exception")
    @Test
    void noopRethrowsTheBodysException() {
        Telemetry noop = Telemetry.noop();

        Assertions.assertThrows(IllegalStateException.class, () -> noop.inSpan("test.op", Attributes.empty(), () -> {
            throw new IllegalStateException("boom");
        }));
    }

    @DisplayName("Two TestTelemetry instances do not see each other's spans or counters")
    @Test
    void twoTestTelemetryInstancesAreIndependent() {
        try (TestTelemetry first = TestTelemetry.create(); TestTelemetry second = TestTelemetry.create()) {
            first.telemetry().inSpan("first.op", Attributes.empty(), () -> first.telemetry().meter().counterBuilder("first.count").build().add(1));

            Assertions.assertEquals(1, first.spans().size());
            Assertions.assertTrue(second.spans().isEmpty(), "the second instance must not see the first one's span");
            Assertions.assertEquals(1, first.counter("first.count", Attributes.empty()));
            Assertions.assertEquals(0, second.counter("first.count", Attributes.empty()), "the second instance must not see the first one's counter");
        }
    }
}
