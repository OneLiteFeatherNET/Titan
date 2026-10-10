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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TestTelemetryTest {

    private static final String HISTOGRAM = "test.histogram";
    private static final Attributes MEDIUM = mode("MEDIUM");
    private static final Attributes HARD = mode("HARD");

    private final TestTelemetry testTelemetry = TestTelemetry.create();

    @AfterEach
    void close() {
        testTelemetry.close();
    }

    private static Attributes mode(String mode) {
        return Attributes.of(AttributeKey.stringKey("mode"), mode);
    }

    private static LongHistogram histogram(TestTelemetry telemetry) {
        return telemetry.telemetry().meter().histogramBuilder(HISTOGRAM).ofLongs().build();
    }

    @Test
    void aHistogramReportsCountAndSumPerAttributeSet() {
        LongHistogram histogram = histogram(testTelemetry);
        histogram.record(3, MEDIUM);
        histogram.record(4, MEDIUM);
        histogram.record(10, HARD);

        assertEquals(new TestTelemetry.HistogramReading(2, 7.0), testTelemetry.histogram(HISTOGRAM, MEDIUM), "medium");
        assertEquals(new TestTelemetry.HistogramReading(1, 10.0), testTelemetry.histogram(HISTOGRAM, HARD), "hard");
    }

    @Test
    void aHistogramNeverRecordedReadsAsZero() {
        assertEquals(new TestTelemetry.HistogramReading(0, 0.0), testTelemetry.histogram(HISTOGRAM, MEDIUM));
    }

    @Test
    void anAttributeSetNeverRecordedReadsAsZero() {
        histogram(testTelemetry).record(3, MEDIUM);

        assertEquals(new TestTelemetry.HistogramReading(0, 0.0), testTelemetry.histogram(HISTOGRAM, HARD));
    }

    @Test
    void theHistogramPointsHoldOnePointPerAttributeSet() {
        LongHistogram histogram = histogram(testTelemetry);
        histogram.record(3, MEDIUM);
        histogram.record(4, MEDIUM);
        histogram.record(10, HARD);

        List<HistogramPointData> points = testTelemetry.histogramPoints(HISTOGRAM);

        assertEquals(2, points.size(), "one point per attribute set");
        assertTrue(points.stream().anyMatch(point -> point.getAttributes().equals(MEDIUM)), "medium point");
        assertTrue(points.stream().anyMatch(point -> point.getAttributes().equals(HARD)), "hard point");
    }

    @Test
    void anotherTelemetryDoesNotSeeTheHistogram() {
        try (TestTelemetry other = TestTelemetry.create()) {
            histogram(testTelemetry).record(3, MEDIUM);
            histogram(other);

            assertTrue(other.histogramPoints(HISTOGRAM).isEmpty(), "the other instance has no point");
            assertEquals(new TestTelemetry.HistogramReading(0, 0.0), other.histogram(HISTOGRAM, MEDIUM));
        }
    }

    @Test
    void readingTwiceReportsCumulativeValues() {
        LongHistogram histogram = histogram(testTelemetry);
        histogram.record(3, MEDIUM);
        TestTelemetry.HistogramReading first = testTelemetry.histogram(HISTOGRAM, MEDIUM);
        histogram.record(4, MEDIUM);

        assertEquals(new TestTelemetry.HistogramReading(1, 3.0), first, "first read");
        assertEquals(new TestTelemetry.HistogramReading(2, 7.0), testTelemetry.histogram(HISTOGRAM, MEDIUM), "second read adds, it does not restart");
        assertEquals(new TestTelemetry.HistogramReading(2, 7.0), testTelemetry.histogram(HISTOGRAM, MEDIUM), "a read without new values changes nothing");
    }
}
