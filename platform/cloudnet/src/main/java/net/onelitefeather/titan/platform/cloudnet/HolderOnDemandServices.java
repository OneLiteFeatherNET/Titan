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
package net.onelitefeather.titan.platform.cloudnet;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.onelitefeather.titan.common.deliver.ServiceControl;
import net.onelitefeather.titan.common.deliver.TitanServiceControl;
import net.onelitefeather.titan.core.service.OnDemandServices;
import net.onelitefeather.titan.core.service.ServiceState;
import net.onelitefeather.titan.core.service.ServiceState.Phase;

/**
 * {@link OnDemandServices} over the CloudNet control of the bridge. It turns the JDK-typed rows of
 * {@link TitanServiceControl} into {@link ServiceState}s. Without a bridge, reads come back
 * unreadable and every request is refused.
 */
public final class HolderOnDemandServices implements OnDemandServices {

    @Override
    public Optional<List<ServiceState>> services(String task) {
        return TitanServiceControl.servicesByTask(task).map(rows -> rows.stream().map(HolderOnDemandServices::state).toList());
    }

    @Override
    public boolean start(String task) {
        return TitanServiceControl.startTask(task);
    }

    @Override
    public boolean stop(String serviceName) {
        return TitanServiceControl.stopService(serviceName);
    }

    private static ServiceState state(Map<String, Object> row) {
        return new ServiceState((String) row.get(ServiceControl.NAME), phase(row.get(ServiceControl.PHASE)), count(row.get(ServiceControl.ONLINE)), count(row.get(ServiceControl.MAX)));
    }

    // Anything the bridge does not know yet counts as starting: it is never ready, and the idle stop clears it.
    private static Phase phase(Object name) {
        return switch (String.valueOf(name)) {
            case "RUNNING" -> Phase.RUNNING;
            case "STOPPED" -> Phase.STOPPED;
            default -> Phase.STARTING;
        };
    }

    private static int count(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
