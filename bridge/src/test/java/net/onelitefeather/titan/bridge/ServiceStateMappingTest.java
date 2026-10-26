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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceStateMappingTest {

    @DisplayName("A prepared service is starting, it is requested but not yet running")
    @Test
    void preparedIsStarting() {
        assertEquals(Phase.STARTING, ServiceStateMapping.phase("PREPARED"), "PREPARED is the state of a requested service");
    }

    @DisplayName("A running service is running")
    @Test
    void runningIsRunning() {
        assertEquals(Phase.RUNNING, ServiceStateMapping.phase("RUNNING"), "RUNNING maps to RUNNING");
    }

    @DisplayName("A stopped or deleted service is stopped")
    @Test
    void stoppedAndDeletedAreStopped() {
        assertEquals(Phase.STOPPED, ServiceStateMapping.phase("STOPPED"), "STOPPED maps to STOPPED");
        assertEquals(Phase.STOPPED, ServiceStateMapping.phase("DELETED"), "DELETED maps to STOPPED");
    }

    @DisplayName("An unknown or missing lifecycle counts as starting, so it is never taken for a free server")
    @Test
    void unknownAndMissingAreStarting() {
        assertEquals(Phase.STARTING, ServiceStateMapping.phase("HIBERNATING"), "a lifecycle this bridge does not know yet");
        assertEquals(Phase.STARTING, ServiceStateMapping.phase(null), "a service without a lifecycle");
    }
}
