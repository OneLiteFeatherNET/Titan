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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServiceTotalsTest {

    @DisplayName("The counts of all running services are summed")
    @Test
    void sumsRunningServices() {
        int[] total = ServiceTotals.total(List.of(new ServiceReading(true, 3, 20), new ServiceReading(true, 4, 30)));

        assertArrayEquals(new int[]{7, 50}, total, "online and max of both services");
    }

    @DisplayName("Services that do not run are ignored")
    @Test
    void ignoresStoppedServices() {
        int[] total = ServiceTotals.total(List.of(new ServiceReading(true, 3, 20), new ServiceReading(false, 9, 99)));

        assertArrayEquals(new int[]{3, 20}, total, "the stopped service must not count");
    }

    @DisplayName("Nothing running is reported as null, not as zero players")
    @Test
    void noneRunningIsNull() {
        assertNull(ServiceTotals.total(List.of(new ServiceReading(false, 1, 2))), "only stopped services");
        assertNull(ServiceTotals.total(List.of()), "no services at all");
    }

    @DisplayName("A running service with no players still counts as running")
    @Test
    void emptyRunningServiceIsRunning() {
        assertArrayEquals(new int[]{0, 20}, ServiceTotals.total(List.of(new ServiceReading(true, 0, 20))), "0 online of 20");
    }

    @DisplayName("A lookup that throws is reported as not running")
    @Test
    void throwingLookupIsNotRunning() {
        assertNull(ServiceTotals.totalOrNotRunning(() -> {
            throw new IllegalStateException("cloud unreachable");
        }), "the failure must not escape the lookup");
    }

    @DisplayName("A lookup that works is summed as usual")
    @Test
    void workingLookupIsSummed() {
        assertArrayEquals(new int[]{3, 20}, ServiceTotals.totalOrNotRunning(() -> List.of(new ServiceReading(true, 3, 20))), "online and max");
    }
}
