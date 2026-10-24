/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.feature.tag;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import java.util.Objects;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Counters of the tag game. Hits are frequent and players are not identifiers worth exporting, so
 * the counters carry only the result or the state, never a player id, and no span is created.
 */
final class TagTelemetry {

    static final AttributeKey<String> RESULT = AttributeKey.stringKey("result");
    static final AttributeKey<String> STATE = AttributeKey.stringKey("state");

    private static final Attributes TAGGED = Attributes.of(RESULT, "tagged");
    private static final Attributes COOLDOWN = Attributes.of(RESULT, "cooldown");
    private static final Attributes ON = Attributes.of(STATE, "on");
    private static final Attributes OFF = Attributes.of(STATE, "off");

    private final LongCounter tags;
    private final LongCounter toggles;

    TagTelemetry(Telemetry telemetry) {
        Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.tags = telemetry.meter().counterBuilder("titan.tag.tags").setUnit("{hit}").setDescription("Hits between players in the tag game").build();
        this.toggles = telemetry.meter().counterBuilder("titan.tag.toggles").setUnit("{toggle}").setDescription("Switches of the tag setting").build();
    }

    /** A hit passed the role on. */
    void tagged() {
        this.tags.add(1, TAGGED);
    }

    /** A reverse hit within the cooldown was refused. */
    void cooldown() {
        this.tags.add(1, COOLDOWN);
    }

    void toggled(boolean on) {
        this.toggles.add(1, on ? ON : OFF);
    }
}
