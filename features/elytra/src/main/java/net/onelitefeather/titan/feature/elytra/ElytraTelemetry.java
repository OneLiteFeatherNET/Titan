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
package net.onelitefeather.titan.feature.elytra;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Spans and counters of the elytra feature. A glide is one span, opened when it ends, with its
 * duration; the ticks of a flight and the burn of each rocket have none. Boosts are counted, since
 * a player can light several per flight.
 */
@Singleton
final class ElytraTelemetry {

    static final String START_SPAN = "elytra.glide.start";
    static final String END_SPAN = "elytra.glide.end";

    private static final AttributeKey<Long> DURATION_MS = AttributeKey.longKey("elytra.glide.duration_ms");
    private static final AttributeKey<String> EVENT = AttributeKey.stringKey("event");

    private static final String STARTED = "started";
    private static final String LANDED = "landed";

    private final Telemetry telemetry;
    private final LongCounter flights;
    private final LongCounter boosts;

    @Inject
    ElytraTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
        this.flights = telemetry.meter().counterBuilder("titan.elytra.flights").setUnit("{flight}").build();
        this.boosts = telemetry.meter().counterBuilder("titan.elytra.boosts").setUnit("{boost}").build();
    }

    void glideStarted(UUID player, Runnable handOutRocket) {
        this.telemetry.inSpan(START_SPAN, Attributes.of(Telemetry.FEATURE, ElytraModule.ID, Telemetry.USER_ID, player.toString()), handOutRocket);
        this.flights.add(1, Attributes.of(EVENT, STARTED));
    }

    void glideEnded(UUID player, long durationMillis, Runnable takeRocketAway) {
        Attributes attributes = Attributes.of(Telemetry.FEATURE, ElytraModule.ID, Telemetry.USER_ID, player.toString(), DURATION_MS, durationMillis);
        this.telemetry.inSpan(END_SPAN, attributes, takeRocketAway);
        this.flights.add(1, Attributes.of(EVENT, LANDED));
    }

    void boosted() {
        this.boosts.add(1);
    }
}
