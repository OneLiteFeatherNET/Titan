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
package net.onelitefeather.titan.feature.sit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Spans and session counter of the sit feature. A sit and a stand-up are rare enough for a span
 * each; the sneak packets sent while walking are not, so they only ever reach the stand-up span
 * when a seat is actually left.
 */
final class SitTelemetry {

    static final String START_SPAN = "sit.start";
    static final String STOP_SPAN = "sit.stop";

    private static final AttributeKey<String> BLOCK = AttributeKey.stringKey("sit.block");
    private static final AttributeKey<String> STOP_REASON = AttributeKey.stringKey("sit.stop.reason");
    private static final AttributeKey<String> EVENT = AttributeKey.stringKey("event");

    private static final String STARTED = "started";
    private static final String STOPPED = "stopped";

    /** Why a player left a seat; the label is the span attribute value. */
    enum StopReason {
        SNEAK("sneak"), DISMOUNT("dismount"), DISCONNECT("disconnect");

        private final String label;

        StopReason(String label) {
            this.label = label;
        }
    }

    private final Telemetry telemetry;
    private final LongCounter sessions;

    SitTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
        this.sessions = telemetry.meter().counterBuilder("titan.sit.sessions").setUnit("{session}").build();
    }

    void sit(UUID player, Key block, Runnable sitDown) {
        Attributes attributes = Attributes.of(Telemetry.FEATURE, SitModule.ID, Telemetry.USER_ID, player.toString(), BLOCK, block.asString());
        this.telemetry.inSpan(START_SPAN, attributes, sitDown);
        this.sessions.add(1, Attributes.of(EVENT, STARTED));
    }

    void standUp(UUID player, StopReason reason, Runnable standUp) {
        Attributes attributes = Attributes.of(Telemetry.FEATURE, SitModule.ID, Telemetry.USER_ID, player.toString(), STOP_REASON, reason.label);
        this.telemetry.inSpan(STOP_SPAN, attributes, standUp);
        this.sessions.add(1, Attributes.of(EVENT, STOPPED));
    }
}
