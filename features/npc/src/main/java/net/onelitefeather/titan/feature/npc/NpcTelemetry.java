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
package net.onelitefeather.titan.feature.npc;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import java.util.Objects;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Metrics of the NPC and ambience modules. Clicks and apparitions are counted; no spans, since an
 * ambience zone runs all the time and a click is cheap.
 */
final class NpcTelemetry {

    private static final AttributeKey<String> NPC = AttributeKey.stringKey("npc");
    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("result");
    private static final AttributeKey<String> ZONE = AttributeKey.stringKey("zone");

    private final LongCounter interactions;
    private final LongCounter apparitions;

    NpcTelemetry(Telemetry telemetry) {
        Objects.requireNonNull(telemetry, "telemetry");
        this.interactions = telemetry.meter().counterBuilder("titan.npc.interactions").setUnit("{click}").build();
        this.apparitions = telemetry.meter().counterBuilder("titan.ambience.apparitions").setUnit("{apparition}").build();
    }

    /** One click on the NPC {@code npc}, with its outcome. */
    void interaction(String npc, String result) {
        this.interactions.add(1, Attributes.builder().put(NPC, npc).put(RESULT, result).build());
    }

    /** One silhouette shown to a player in the zone {@code zone}. */
    void apparition(String zone) {
        this.apparitions.add(1, Attributes.of(ZONE, zone));
    }
}
