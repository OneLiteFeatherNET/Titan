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
package net.onelitefeather.titan.feature.spawn;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import net.onelitefeather.titan.core.module.SpawnReturn;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * Spans and counters of the spawn column. A return is rare enough for a span; the height check
 * runs on every move, so it only produces a span in its teleport branch.
 */
final class SpawnTelemetry {

    static final String RETURN_SPAN = "spawn.return";
    static final String BOUNDS_TELEPORT_SPAN = "spawn.bounds_teleport";
    static final String JOIN_SPAN = "spawn.join";

    static final AttributeKey<String> RETURN_SOURCE = AttributeKey.stringKey("spawn.return.source");
    static final AttributeKey<String> RETURN_RESULT = AttributeKey.stringKey("spawn.return.result");
    static final AttributeKey<Double> Y = AttributeKey.doubleKey("spawn.y");
    static final AttributeKey<Long> MIN_HEIGHT = AttributeKey.longKey("spawn.min_height");
    static final AttributeKey<Long> MAX_HEIGHT = AttributeKey.longKey("spawn.max_height");

    private static final AttributeKey<String> METRIC_SOURCE = AttributeKey.stringKey("source");
    private static final AttributeKey<String> METRIC_RESULT = AttributeKey.stringKey("result");

    private static final String SENT = "sent";
    private static final String BLOCKED = "blocked";

    private final Telemetry telemetry;
    private final LongCounter returns;
    private final LongCounter boundsTeleports;

    SpawnTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
        this.returns = telemetry.meter().counterBuilder("titan.spawn.returns").setUnit("{return}").build();
        this.boundsTeleports = telemetry.meter().counterBuilder("titan.spawn.bounds_teleports").setUnit("{teleport}").build();
    }

    /**
     * Runs {@code body} in a span carrying the source and the player's UUID, then records the
     * outcome on the span and in the counter. {@code NO_SPAWN} means the return was blocked.
     */
    SpawnReturn.Result inReturn(UUID player, SpawnReturn.Source source, Supplier<SpawnReturn.Result> body) {
        String sourceValue = source.name().toLowerCase(Locale.ROOT);
        Attributes attributes = Attributes.builder().put(RETURN_SOURCE, sourceValue).put(Telemetry.USER_ID, player.toString()).build();
        return this.telemetry.inSpan(RETURN_SPAN, attributes, () -> {
            SpawnReturn.Result result = body.get();
            String outcome = result == SpawnReturn.Result.RETURNED ? SENT : BLOCKED;
            Span.current().setAttribute(RETURN_RESULT, outcome);
            this.returns.add(1, Attributes.of(METRIC_SOURCE, sourceValue, METRIC_RESULT, outcome));
            return result;
        });
    }

    /** Runs the teleport back to spawn inside the bounds span and counts it. */
    void inBoundsTeleport(double y, int minHeight, int maxHeight, Runnable teleport) {
        Attributes attributes = Attributes.builder().put(Y, y).put(MIN_HEIGHT, (long) minHeight).put(MAX_HEIGHT, (long) maxHeight).build();
        this.telemetry.inSpan(BOUNDS_TELEPORT_SPAN, attributes, teleport);
        this.boundsTeleports.add(1);
    }
}
