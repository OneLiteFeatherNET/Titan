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

import java.util.List;
import java.util.Map;
import net.onelitefeather.titan.common.deliver.PlayerCountLookup;
import net.onelitefeather.titan.common.deliver.ServiceControl;
import net.onelitefeather.titan.core.service.ServiceState.Phase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceListingTest {

    private static Map<String, Object> row(String name, int online, int max) {
        return Map.of(PlayerCountLookup.NAME, name, PlayerCountLookup.ONLINE, online, PlayerCountLookup.MAX, max);
    }

    @DisplayName("A running service keeps its name, online and max")
    @Test
    void keepsNameOnlineAndMax() {
        List<Map<String, Object>> listed = ServiceListing.running(List.of(new ServiceReading("Lobby-1", Phase.RUNNING, 3, 20)));

        assertEquals(List.of(row("Lobby-1", 3, 20)), listed, "the one running service");
    }

    @DisplayName("Services that do not run are left out")
    @Test
    void leavesOutStoppedServices() {
        List<Map<String, Object>> listed = ServiceListing.running(List.of(new ServiceReading("Lobby-1", Phase.RUNNING, 3, 20), new ServiceReading("Lobby-2", Phase.STOPPED, 9, 99)));

        assertEquals(List.of(row("Lobby-1", 3, 20)), listed, "the stopped service must not be listed");
    }

    @DisplayName("The order of the readings is kept")
    @Test
    void keepsOrder() {
        List<Map<String, Object>> listed = ServiceListing.running(List.of(new ServiceReading("Lobby-2", Phase.RUNNING, 1, 20), new ServiceReading("Lobby-1", Phase.RUNNING, 2, 20)));

        assertEquals(List.of("Lobby-2", "Lobby-1"), listed.stream().map(row -> (String) row.get(PlayerCountLookup.NAME)).toList(), "no sorting here");
    }

    @DisplayName("Nothing running lists nothing")
    @Test
    void nothingRunningIsEmpty() {
        assertTrue(ServiceListing.running(List.of(new ServiceReading("Lobby-1", Phase.STOPPED, 1, 2))).isEmpty(), "only stopped services");
        assertTrue(ServiceListing.running(List.of()).isEmpty(), "no services at all");
    }

    @DisplayName("The quick join reads running services only, so a starting service is left out")
    @Test
    void quickJoinSkipsStartingServices() {
        List<Map<String, Object>> listed = ServiceListing.running(List.of(new ServiceReading("Slender-Staging-1", Phase.STARTING, 0, 20), new ServiceReading("Slender-Staging-2", Phase.RUNNING, 3, 20)));

        assertEquals(List.of(row("Slender-Staging-2", 3, 20)), listed, "a starting service is not joinable");
    }

    @DisplayName("The on-demand listing keeps starting services and names their phase")
    @Test
    void withPhasesKeepsStartingServices() {
        List<Map<String, Object>> listed = ServiceListing.withPhases(List.of(new ServiceReading("Slender-Staging-1", Phase.STARTING, 0, 0), new ServiceReading("Slender-Staging-2", Phase.RUNNING, 3, 20)));

        assertEquals(List.of(phaseRow("Slender-Staging-1", "STARTING", 0, 0), phaseRow("Slender-Staging-2", "RUNNING", 3, 20)), listed, "both services, each with its phase name");
    }

    private static Map<String, Object> phaseRow(String name, String phase, int online, int max) {
        return Map.of(ServiceControl.NAME, name, ServiceControl.PHASE, phase, ServiceControl.ONLINE, online, ServiceControl.MAX, max);
    }
}
