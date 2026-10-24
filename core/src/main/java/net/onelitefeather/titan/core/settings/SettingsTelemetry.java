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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import java.util.Objects;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Metrics for the player settings. Player UUIDs and values never appear here, only the setting's
 * key, which is a fixed name.
 */
public final class SettingsTelemetry {

    public static final String OK = "ok";

    static final AttributeKey<String> OUTCOME = AttributeKey.stringKey("outcome");
    static final AttributeKey<String> KEY = AttributeKey.stringKey("key");
    static final AttributeKey<String> STATE = AttributeKey.stringKey("state");
    static final AttributeKey<String> STORE = AttributeKey.stringKey("store");

    private final DoubleHistogram loads;
    private final LongCounter writes;
    private final LongCounter toggles;
    private final LongCounter circuit;
    private final LongCounter started;

    /** Creates the settings instruments on the telemetry's meter. */
    public SettingsTelemetry(Telemetry telemetry) {
        Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.loads = telemetry.meter().histogramBuilder("titan.settings.load").setUnit("ms").build();
        this.writes = telemetry.meter().counterBuilder("titan.settings.write").setUnit("{write}").build();
        this.toggles = telemetry.meter().counterBuilder("titan.settings.toggled").setUnit("{toggle}").build();
        this.circuit = telemetry.meter().counterBuilder("titan.settings.circuit").setUnit("{transition}").build();
        this.started = telemetry.meter().counterBuilder("titan.settings.started").setUnit("{start}").build();
    }

    /** @param outcome {@value #OK} or the {@link SettingsStoreException.Failure#outcome()} */
    public void loaded(long millis, String outcome) {
        this.loads.record(millis, Attributes.of(OUTCOME, outcome));
    }

    /** @param outcome {@code ok}, {@code retry} while a retry is queued, or {@code error} */
    public void written(String outcome) {
        this.writes.add(1, Attributes.of(OUTCOME, outcome));
    }

    /** @param key the setting that the player toggled */
    public void toggled(SettingKey key) {
        this.toggles.add(1, Attributes.of(KEY, key.toString()));
    }

    /** @param state {@code open}, {@code half_open} or {@code closed} */
    public void circuitChanged(String state) {
        this.circuit.add(1, Attributes.of(STATE, state));
    }

    /** @param store {@code memory} or {@code otis}, recorded once when the feature starts */
    public void started(String store) {
        this.started.add(1, Attributes.of(STORE, store));
    }
}
