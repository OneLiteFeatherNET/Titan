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
import net.onelitefeather.titan.common.deliver.PlayerCountLookup;
import net.onelitefeather.titan.common.deliver.ServiceControl;

/**
 * Lists the services of a source one by one, as JDK-typed rows. Kept free of CloudNet types so it
 * is testable on its own.
 */
final class ServiceListing {

    private ServiceListing() {
    }

    /**
     * The running services in the order given, as rows keyed by {@link PlayerCountLookup}; services
     * that do not run are left out. The quick join and the player counts read only these.
     */
    static List<Map<String, Object>> running(Collection<ServiceReading> services) {
        return services.stream().filter(ServiceReading::running).map(ServiceListing::countRow).toList();
    }

    /**
     * Every service in the order given, starting ones included, as rows keyed by
     * {@link ServiceControl} with the phase name; the on-demand staging reads these.
     */
    static List<Map<String, Object>> withPhases(Collection<ServiceReading> services) {
        return services.stream().map(ServiceListing::phaseRow).toList();
    }

    private static Map<String, Object> countRow(ServiceReading service) {
        return Map.of(PlayerCountLookup.NAME, service.name(), PlayerCountLookup.ONLINE, service.online(), PlayerCountLookup.MAX, service.max());
    }

    private static Map<String, Object> phaseRow(ServiceReading service) {
        return Map.of(ServiceControl.NAME, service.name(), ServiceControl.PHASE, service.phase().name(), ServiceControl.ONLINE, service.online(), ServiceControl.MAX, service.max());
    }
}
