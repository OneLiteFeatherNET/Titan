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
package net.onelitefeather.titan.feature.throwing;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.feature.throwing.ThrowRules.Refusal;

/** Counts the throw's events. Player ids never appear in an attribute. */
final class ThrowTelemetry {

    private static final AttributeKey<String> REASON = AttributeKey.stringKey("reason");

    private final LongCounter pickups;
    private final LongCounter throwsCounter;
    private final LongCounter escapes;
    private final LongCounter refusals;
    private final LongCounter chargeAborts;

    ThrowTelemetry(Telemetry telemetry) {
        this.pickups = telemetry.meter().counterBuilder("titan.throw.pickups").setUnit("{pickup}").build();
        this.throwsCounter = telemetry.meter().counterBuilder("titan.throw.throws").setUnit("{throw}").build();
        this.escapes = telemetry.meter().counterBuilder("titan.throw.escapes").setUnit("{escape}").build();
        this.refusals = telemetry.meter().counterBuilder("titan.throw.refusals").setUnit("{refusal}").build();
        this.chargeAborts = telemetry.meter().counterBuilder("titan.throw.charge_aborts").setUnit("{abort}").build();
    }

    void pickedUp() {
        this.pickups.add(1);
    }

    void thrown() {
        this.throwsCounter.add(1);
    }

    /** The target sneaked out of the carry. */
    void escaped() {
        this.escapes.add(1);
    }

    void refused(Refusal refusal) {
        this.refusals.add(1, Attributes.of(REASON, refusal.reason()));
    }

    /** The thrower changed the hotbar slot during a charge. */
    void chargeAborted() {
        this.chargeAborts.add(1);
    }
}
