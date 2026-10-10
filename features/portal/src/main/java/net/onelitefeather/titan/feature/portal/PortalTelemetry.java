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
package net.onelitefeather.titan.feature.portal;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Spans and metrics of the portal column. A span marks a transfer that is actually sent and one
 * label cycle; walking through a portal, a refused entry and each player-count read only count,
 * since a move happens far too often for a span. The player is a span attribute, as a UUID.
 */
final class PortalTelemetry {

    static final String TRANSFER_SPAN = "portal.transfer";
    static final String LABELS_SPAN = "portal.labels.refresh";

    private static final AttributeKey<String> PORTAL_ID = AttributeKey.stringKey("portal.id");
    private static final AttributeKey<String> PORTAL_TASK = AttributeKey.stringKey("portal.task");
    private static final AttributeKey<String> PORTAL_RESULT = AttributeKey.stringKey("portal.result");
    private static final AttributeKey<Long> LABEL_COUNT = AttributeKey.longKey("portal.labels.count");
    private static final AttributeKey<Long> LABEL_FAILED = AttributeKey.longKey("portal.labels.failed");
    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("result");

    private static final String DELIVERED = "delivered";
    private static final String OK = "ok";
    private static final String ERROR = "error";

    private final Telemetry telemetry;
    private final LongCounter transfers;
    private final LongCounter denied;
    private final LongCounter lookups;

    PortalTelemetry(Telemetry telemetry) {
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
        this.transfers = telemetry.meter().counterBuilder("portal.transfers").setUnit("{transfer}").build();
        this.denied = telemetry.meter().counterBuilder("portal.denied").setUnit("{entry}").build();
        this.lookups = telemetry.meter().counterBuilder("portal.player_count.lookups").setUnit("{lookup}").build();
    }

    /**
     * Runs {@code send}, the hand-over of one player, in a {@link #TRANSFER_SPAN} span. The result
     * is set on the span and counted; a failure is rethrown to the caller.
     */
    void transfer(UUID player, Portal portal, Runnable send) {
        Attributes attributes = Attributes.builder().put(PORTAL_ID, portal.id()).put(PORTAL_TASK, portal.task()).put(Telemetry.USER_ID, player.toString()).build();
        this.telemetry.inSpan(TRANSFER_SPAN, attributes, () -> {
            try {
                send.run();
                recordTransfer(DELIVERED);
            } catch (RuntimeException e) {
                recordTransfer(ERROR);
                throw e;
            }
        });
    }

    private void recordTransfer(String result) {
        Span.current().setAttribute(PORTAL_RESULT, result);
        this.transfers.add(1, Attributes.of(RESULT, result));
    }

    /** A player entered {@code portal} without the permission for it. */
    void denied(Portal portal) {
        this.denied.add(1, Attributes.of(PORTAL_ID, portal.id()));
    }

    /** One player-count read, with its result. */
    void lookup(boolean succeeded) {
        this.lookups.add(1, Attributes.of(RESULT, succeeded ? OK : ERROR));
    }

    /**
     * Runs one label cycle in a {@link #LABELS_SPAN} span. {@code cycle} returns one entry per
     * label; those for which {@code failed} holds are counted on the span.
     */
    <T> List<T> refreshLabels(int labels, Supplier<List<T>> cycle, Predicate<T> failed) {
        return this.telemetry.inSpan(LABELS_SPAN, Attributes.of(LABEL_COUNT, (long) labels), () -> {
            List<T> results = cycle.get();
            Span.current().setAttribute(LABEL_FAILED, results.stream().filter(failed).count());
            return results;
        });
    }
}
