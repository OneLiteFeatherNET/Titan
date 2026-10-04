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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import java.util.function.Supplier;

/**
 * The lobby's tracer and meter in one bean, so a feature asks for a single dependency. Backed by
 * the OpenTelemetry Java agent when one is attached, a no-op otherwise.
 */
public record Telemetry(Tracer tracer, Meter meter) {

    /** The instrumentation scope every Titan span and metric is reported under. */
    public static final String SCOPE = "net.onelitefeather.titan";

    /** The feature a span or metric belongs to. */
    public static final AttributeKey<String> FEATURE = AttributeKey.stringKey("titan.feature");

    public static final AttributeKey<Long> FEATURE_PRIORITY = AttributeKey.longKey("titan.feature.priority");

    /** The player's UUID; never the name (see the privacy rule in docs/lobby-modules.md). */
    public static final AttributeKey<String> USER_ID = AttributeKey.stringKey("user.id");

    public static Telemetry of(OpenTelemetry openTelemetry) {
        return new Telemetry(openTelemetry.getTracer(SCOPE), openTelemetry.getMeter(SCOPE));
    }

    public static Telemetry noop() {
        return of(OpenTelemetry.noop());
    }

    /**
     * Runs {@code body} in a span that is current for its duration. An exception is recorded on the
     * span, which gets status ERROR, and rethrown; the span always ends.
     */
    public <T> T inSpan(String name, Attributes attributes, Supplier<T> body) {
        var span = this.tracer.spanBuilder(name).setAllAttributes(attributes).startSpan();
        try (var ignored = span.makeCurrent()) {
            return body.get();
        } catch (Throwable throwable) {
            span.recordException(throwable);
            span.setStatus(StatusCode.ERROR);
            throw throwable;
        } finally {
            span.end();
        }
    }

    public void inSpan(String name, Attributes attributes, Runnable body) {
        inSpan(name, attributes, () -> {
            body.run();
            return null;
        });
    }
}
