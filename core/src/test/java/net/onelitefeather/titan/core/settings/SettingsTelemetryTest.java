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
package net.onelitefeather.titan.core.settings;

import io.opentelemetry.api.common.Attributes;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SettingsTelemetryTest {

    private static final SettingKey KEY = SettingKey.parse("titan:tag.enabled");

    @DisplayName("A load is recorded in the histogram under its outcome")
    @Test
    void loadIsRecordedWithOutcome() {
        try (TestTelemetry test = TestTelemetry.create()) {
            new SettingsTelemetry(test.telemetry()).loaded(120, SettingsTelemetry.OK);
            TestTelemetry.HistogramReading reading = test.histogram("titan.settings.load", Attributes.builder().put("outcome", "ok").build());
            Assertions.assertEquals(1, reading.count(), "one load must be recorded once");
            Assertions.assertEquals(120.0, reading.sum(), 0.0001, "the recorded duration must be the millis given");
        }
    }

    @DisplayName("A toggle counts per key and carries no player id")
    @Test
    void toggleCountsPerKey() {
        try (TestTelemetry test = TestTelemetry.create()) {
            new SettingsTelemetry(test.telemetry()).toggled(KEY);
            Assertions.assertEquals(1, test.counter("titan.settings.toggled", Attributes.builder().put("key", "titan:tag.enabled").build()), "the toggle must count under its key");
        }
    }

    @DisplayName("A write outcome is counted under its outcome")
    @Test
    void writeOutcomeIsCounted() {
        try (TestTelemetry test = TestTelemetry.create()) {
            new SettingsTelemetry(test.telemetry()).written("retry");
            Assertions.assertEquals(1, test.counter("titan.settings.write", Attributes.builder().put("outcome", "retry").build()), "a retried write must count as retry");
        }
    }

    @DisplayName("A breaker transition is counted under its state")
    @Test
    void circuitTransitionIsCounted() {
        try (TestTelemetry test = TestTelemetry.create()) {
            new SettingsTelemetry(test.telemetry()).circuitChanged("open");
            Assertions.assertEquals(1, test.counter("titan.settings.circuit", Attributes.builder().put("state", "open").build()), "an opened breaker must count as open");
        }
    }
}
