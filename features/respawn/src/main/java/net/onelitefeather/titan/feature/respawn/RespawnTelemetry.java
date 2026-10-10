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
package net.onelitefeather.titan.feature.respawn;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import java.util.UUID;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/** The span and counter of a respawn; a death happens rarely enough for both. */
final class RespawnTelemetry {

    static final String RESPAWN_SPAN = "respawn.perform";

    private final Telemetry telemetry;
    private final LongCounter respawns;

    RespawnTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
        this.respawns = telemetry.meter().counterBuilder("player.respawns").setUnit("{respawn}").build();
    }

    void respawn(UUID player, Runnable respawn) {
        this.telemetry.inSpan(RESPAWN_SPAN, Attributes.of(Telemetry.FEATURE, RespawnModule.ID, Telemetry.USER_ID, player.toString()), respawn);
        this.respawns.add(1);
    }
}
