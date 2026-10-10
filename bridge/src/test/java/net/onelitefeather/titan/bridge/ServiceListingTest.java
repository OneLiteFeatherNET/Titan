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
import net.onelitefeather.titan.bridge.ServiceTotals.ServiceReading;
import net.onelitefeather.titan.core.portal.ServiceCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceListingTest {

    @DisplayName("A running service keeps its name, online and max")
    @Test
    void keepsNameOnlineAndMax() {
        List<ServiceCount> listed = ServiceListing.running(List.of(new ServiceReading("Lobby-1", true, 3, 20)));

        assertEquals(List.of(new ServiceCount("Lobby-1", 3, 20)), listed, "the one running service");
    }

    @DisplayName("Services that do not run are left out")
    @Test
    void leavesOutStoppedServices() {
        List<ServiceCount> listed = ServiceListing.running(List.of(new ServiceReading("Lobby-1", true, 3, 20), new ServiceReading("Lobby-2", false, 9, 99)));

        assertEquals(List.of(new ServiceCount("Lobby-1", 3, 20)), listed, "the stopped service must not be listed");
    }

    @DisplayName("The order of the readings is kept")
    @Test
    void keepsOrder() {
        List<ServiceCount> listed = ServiceListing.running(List.of(new ServiceReading("Lobby-2", true, 1, 20), new ServiceReading("Lobby-1", true, 2, 20)));

        assertEquals(List.of("Lobby-2", "Lobby-1"), listed.stream().map(ServiceCount::name).toList(), "no sorting here");
    }

    @DisplayName("Nothing running lists nothing")
    @Test
    void nothingRunningIsEmpty() {
        assertTrue(ServiceListing.running(List.of(new ServiceReading("Lobby-1", false, 1, 2))).isEmpty(), "only stopped services");
        assertTrue(ServiceListing.running(List.of()).isEmpty(), "no services at all");
    }
}
