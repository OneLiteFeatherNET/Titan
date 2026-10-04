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
package net.onelitefeather.titan.runtime.lifecycle;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import java.util.List;
import java.util.function.Supplier;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Wraps the lobby's start and stop in the {@code titan.startup} and {@code titan.shutdown} spans.
 * Every {@code FeatureNode} attached or closed inside adds its {@code feature.started} or
 * {@code feature.stopped} event to the span, so the start order shows up without a span per
 * feature.
 */
public final class TitanLifecycle {

    private static final AttributeKey<String> VARIANT = AttributeKey.stringKey("titan.variant");
    private static final AttributeKey<List<String>> PROFILES = AttributeKey.stringArrayKey("titan.profiles");
    private static final AttributeKey<Long> MODULES_LOADED = AttributeKey.longKey("titan.modules.loaded");

    private final Telemetry telemetry;

    public TitanLifecycle(Telemetry telemetry) {
        this.telemetry = telemetry;
    }

    /**
     * Runs {@code body} in {@code titan.startup}; a failure is recorded on the span and rethrown.
     */
    public <T> T startup(Supplier<T> body) {
        return this.telemetry.inSpan("titan.startup", Attributes.empty(), body);
    }

    /**
     * Puts the variant, profiles and module count on the running {@code titan.startup} span. They
     * are only known inside the span, since finding them can fail too.
     */
    public static void describeStartup(String variant, List<String> profiles, int modulesLoaded) {
        Span.current().setAllAttributes(Attributes.builder().put(VARIANT, variant).put(PROFILES, profiles).put(MODULES_LOADED, (long) modulesLoaded).build());
    }

    /** Runs {@code body} in {@code titan.shutdown}. */
    public void shutdown(Runnable body) {
        this.telemetry.inSpan("titan.shutdown", Attributes.empty(), body);
    }
}
