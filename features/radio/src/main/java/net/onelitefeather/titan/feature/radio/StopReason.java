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
package net.onelitefeather.titan.feature.radio;

/** Why a title stopped; telemetry counts a stop by it. */
enum StopReason {
    /** The player chose stop. */
    USER("user"),
    /** The player chose next or another disc. */
    NEXT("next"),
    /** The title ended by itself. */
    END("end"),
    /** The player left the server. */
    LOGOUT("logout"),
    /** The player became busy: a jump run or a hand-over to another server. */
    BUSY("busy"),
    /** The lobby is shutting down. */
    SHUTDOWN("shutdown");

    private final String telemetryName;

    StopReason(String telemetryName) {
        this.telemetryName = telemetryName;
    }

    String telemetryName() {
        return this.telemetryName;
    }
}
