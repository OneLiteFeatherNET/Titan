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
import java.util.Map;
import net.onelitefeather.titan.bridge.ServiceTotals.ServiceReading;
import net.onelitefeather.titan.common.deliver.PlayerCountLookup;

/**
 * Lists the running services of a source one by one, where {@link ServiceTotals} sums them. Kept
 * free of CloudNet types so it is testable on its own.
 */
final class ServiceListing {

    private ServiceListing() {
    }

    /**
     * The running services in the order given as JDK-typed rows ({@link PlayerCountLookup#NAME},
     * {@link PlayerCountLookup#ONLINE}, {@link PlayerCountLookup#MAX}); services that do not run
     * are
     * left out.
     */
    static List<Map<String, Object>> running(Collection<ServiceReading> services) {
        return services.stream().filter(ServiceReading::running).map(ServiceListing::row).toList();
    }

    private static Map<String, Object> row(ServiceReading service) {
        return Map.of(PlayerCountLookup.NAME, service.name(), PlayerCountLookup.ONLINE, service.online(), PlayerCountLookup.MAX, service.max());
    }
}
