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
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TitanLifecycleTest {

    private final TestTelemetry test = TestTelemetry.create();
    private final TitanLifecycle lifecycle = new TitanLifecycle(this.test.telemetry());

    @AfterEach
    void closeTelemetry() {
        this.test.close();
    }

    private static List<String> featureIds(SpanData span, String eventName) {
        return span.getEvents().stream().filter(event -> event.getName().equals(eventName)).map(event -> event.getAttributes().get(Telemetry.FEATURE)).toList();
    }

    @DisplayName("titan.startup carries the variant, the profiles and the number of loaded modules")
    @Test
    void startupSpanCarriesVariantProfilesAndModuleCount() {
        this.lifecycle.startup("cloudnet", List.of("dev", "cloudnet"), 14, () -> "scope");

        SpanData span = this.test.span("titan.startup");
        Assertions.assertEquals("cloudnet", this.test.attribute(span, AttributeKey.stringKey("titan.variant")));
        Assertions.assertEquals(List.of("dev", "cloudnet"), this.test.attribute(span, AttributeKey.stringArrayKey("titan.profiles")));
        Assertions.assertEquals(14L, this.test.attribute(span, AttributeKey.longKey("titan.modules.loaded")));
    }

    @DisplayName("startup returns what the body built")
    @Test
    void startupReturnsTheBodysResult() {
        String result = this.lifecycle.startup("local", List.of(), 1, () -> "scope");

        Assertions.assertEquals("scope", result);
    }

    @DisplayName("titan.startup records a feature.started event per feature, in start order")
    @Test
    void startupRecordsFeaturesInStartOrder() {
        EventNode<Event> titan = EventNode.all("titan");

        this.lifecycle.startup("local", List.of(), 2, () -> {
            FeatureNode.attach(titan, "sit", 500, this.test.telemetry());
            FeatureNode.attach(titan, "protection", 100, this.test.telemetry());
            return "scope";
        });

        Assertions.assertEquals(List.of("sit", "protection"), featureIds(this.test.span("titan.startup"), "feature.started"));
    }

    @DisplayName("A failing start leaves the exception and status ERROR on titan.startup and is rethrown")
    @Test
    void failingStartupMarksTheSpanAndRethrows() {
        IllegalStateException failure = new IllegalStateException("feature broke");

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> this.lifecycle.startup("local", List.of(), 1, () -> {
            throw failure;
        }));

        Assertions.assertSame(failure, thrown);
        SpanData span = this.test.span("titan.startup");
        Assertions.assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
        Assertions.assertTrue(span.getEvents().stream().map(EventData::getName).anyMatch("exception"::equals), "the span must carry the exception");
    }

    @DisplayName("titan.shutdown records a feature.stopped event per closed feature")
    @Test
    void shutdownRecordsStoppedFeatures() {
        EventNode<Event> titan = EventNode.all("titan");
        FeatureNode sit = FeatureNode.attach(titan, "sit", 500, this.test.telemetry());
        FeatureNode protection = FeatureNode.attach(titan, "protection", 100, this.test.telemetry());

        this.lifecycle.shutdown(() -> {
            protection.close();
            sit.close();
        });

        Assertions.assertEquals(List.of("protection", "sit"), featureIds(this.test.span("titan.shutdown"), "feature.stopped"));
    }
}
