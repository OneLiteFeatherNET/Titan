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

import java.util.Collection;

/**
 * Sums the player counts of services. Kept free of CloudNet types so it is testable on its own;
 * the extension maps each service snapshot to a {@link ServiceReading}.
 */
final class ServiceTotals {

    /** One service: its name, whether it runs, and the players the bridge reports for it. */
    record ServiceReading(String name, boolean running, int online, int max) {
    }

    private ServiceTotals() {
    }

    /** {@code {online, max}} over the running services, or {@code null} when none runs. */
    static int[] total(Collection<ServiceReading> services) {
        int online = 0;
        int max = 0;
        boolean anyRunning = false;
        for (ServiceReading service : services) {
            if (service.running()) {
                anyRunning = true;
                online += service.online();
                max += service.max();
            }
        }
        return anyRunning ? new int[]{online, max} : null;
    }
}
