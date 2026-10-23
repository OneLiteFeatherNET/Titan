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
package net.onelitefeather.titan.feature.dance;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * The started-dance counter. Dances are started rarely and each one is short, so a counter is
 * enough; spans would add nothing a player can act on.
 */
final class DanceTelemetry {

    static final String STARTED = "titan.dance.started";

    private static final AttributeKey<String> DANCE = AttributeKey.stringKey("dance");

    private final LongCounter started;

    DanceTelemetry(Telemetry telemetry) {
        this.started = telemetry.meter().counterBuilder(STARTED).setUnit("{dance}").build();
    }

    void started(Dance dance) {
        this.started.add(1, Attributes.of(DANCE, dance.id()));
    }
}
