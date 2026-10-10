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
import java.util.List;
import net.onelitefeather.titan.bridge.ServiceTotals.ServiceReading;
import net.onelitefeather.titan.core.portal.ServiceCount;

/**
 * Lists the running services of a source one by one, where {@link ServiceTotals} sums them. Kept
 * free of CloudNet types so it is testable on its own.
 */
final class ServiceListing {

    private ServiceListing() {
    }

    /** The running services in the order given; services that do not run are left out. */
    static List<ServiceCount> running(Collection<ServiceReading> services) {
        return services.stream().filter(ServiceReading::running).map(service -> new ServiceCount(service.name(), service.online(), service.max())).toList();
    }
}
