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
 * One service as the bridge reports it: its name, its phase, and its players. Kept free of
 * CloudNet types so the mapping and listing are testable on their own.
 */
record ServiceReading(String name, Phase phase, int online, int max) {

    /** Whether the service is running; the quick join reads nothing else. */
    boolean running() {
        return this.phase == Phase.RUNNING;
    }
}
