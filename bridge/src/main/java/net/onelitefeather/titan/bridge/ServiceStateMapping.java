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
package net.onelitefeather.titan.bridge;

import net.onelitefeather.titan.core.service.ServiceState.Phase;

/**
 * Maps a CloudNet lifecycle, given by its enum name, to the {@link Phase} Titan needs. Takes the
 * name rather than the CloudNet enum so it is testable without CloudNet on the test classpath.
 */
final class ServiceStateMapping {

    private ServiceStateMapping() {
    }

    /**
     * {@code PREPARED} is a service that is being started. Anything unknown, including a lifecycle
     * CloudNet adds later, counts as starting, so it is never taken for a free server.
     */
    static Phase phase(String lifeCycle) {
        if (lifeCycle == null) {
            return Phase.STARTING;
        }
        return switch (lifeCycle) {
            case "RUNNING" -> Phase.RUNNING;
            case "STOPPED", "DELETED" -> Phase.STOPPED;
            default -> Phase.STARTING;
        };
    }
}
