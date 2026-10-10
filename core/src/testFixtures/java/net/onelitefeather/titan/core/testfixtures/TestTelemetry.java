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
package net.onelitefeather.titan.core.testfixtures;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import java.util.stream.Stream;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * A {@link Telemetry} backed by its own in-memory exporter and metric reader, so a test reads back
 * exactly the spans and counters it produced. Never registers itself globally and records
 * synchronously, so tests neither interfere with each other nor wait. Close it (try-with-resources
 * or {@code @AfterEach}) when the test is done.
 */
public final class TestTelemetry implements AutoCloseable {

    private final InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
    private final InMemoryMetricReader metricReader = InMemoryMetricReader.create();
    private final SdkTracerProvider tracerProvider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(this.spanExporter)).build();
    private final SdkMeterProvider meterProvider = SdkMeterProvider.builder().registerMetricReader(this.metricReader).build();
    private final Telemetry telemetry = new Telemetry(this.tracerProvider.get("test"), this.meterProvider.get("test"));

    private TestTelemetry() {
    }

    public static TestTelemetry create() {
        return new TestTelemetry();
    }

    public Telemetry telemetry() {
        return this.telemetry;
    }

    /** The spans that have ended so far, in end order. */
    public List<SpanData> spans() {
        return this.spanExporter.getFinishedSpanItems();
    }

    /** @throws AssertionError unless exactly one ended span has this name */
    public SpanData span(String name) {
        List<SpanData> matching = spans().stream().filter(span -> span.getName().equals(name)).toList();
        if (matching.size() != 1) {
            throw new AssertionError("expected exactly one span named '" + name + "' but found " + matching.size() + " in " + spans().stream().map(SpanData::getName).toList());
        }
        return matching.getFirst();
    }

    public <T> T attribute(SpanData span, AttributeKey<T> key) {
        return span.getAttributes().get(key);
    }

    /** The counter's value for exactly these attributes, 0 if it was never incremented. */
    public long counter(String name, Attributes attributes) {
        return this.metricReader.collectAllMetrics().stream().filter(metric -> metric.getName().equals(name)).flatMap(metric -> metric.getLongSumData().getPoints().stream()).filter(point -> point.getAttributes().equals(attributes)).mapToLong(LongPointData::getValue).sum();
    }

    /** The current value of a long gauge, summed over its points. */
    public long gauge(String name) {
        return this.metricReader.collectAllMetrics().stream().filter(metric -> metric.getName().equals(name)).flatMap(metric -> metric.getLongGaugeData().getPoints().stream()).mapToLong(LongPointData::getValue).sum();
    }

    /** Every recorded point of a histogram, one per attribute set. */
    public List<HistogramPointData> histogramPoints(String name) {
        return this.metricReader.collectAllMetrics().stream().filter(metric -> metric.getName().equals(name)).flatMap(metric -> metric.getHistogramData().getPoints().stream()).toList();
    }

    /** Count and sum recorded for exactly these attributes, 0 and 0.0 if nothing was recorded. */
    public HistogramReading histogram(String name, Attributes attributes) {
        List<HistogramPointData> matching = histogramPoints(name).stream().filter(point -> point.getAttributes().equals(attributes)).toList();
        return new HistogramReading(matching.stream().mapToLong(HistogramPointData::getCount).sum(), matching.stream().mapToDouble(HistogramPointData::getSum).sum());
    }

    /** Every span name and attribute value so far, for asserting that a value never appears. */
    public List<String> allSpanText() {
        return spans().stream().flatMap(span -> Stream.concat(Stream.of(span.getName()), span.getAttributes().asMap().values().stream().map(String::valueOf))).toList();
    }

    public record HistogramReading(long count, double sum) {
    }

    @Override
    public void close() {
        this.tracerProvider.close();
        this.meterProvider.close();
    }
}
