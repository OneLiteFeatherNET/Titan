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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.onelitefeather.titan.common.deliver.ServiceControl;
import net.onelitefeather.titan.common.deliver.TitanServiceControl;
import net.onelitefeather.titan.core.service.OnDemandServices;
import net.onelitefeather.titan.core.service.ServiceState;
import net.onelitefeather.titan.core.testfixtures.OnDemandServicesContract;
import org.junit.jupiter.api.AfterEach;

/**
 * The real holder, with a fake CloudNet control installed in the holder, against the shared
 * contract.
 */
class HolderOnDemandServicesContractTest implements OnDemandServicesContract {

    // The holder is static by nature; every test leaves it empty again.
    @AfterEach
    void emptyHolder() {
        TitanServiceControl.setControl(null);
    }

    @Override
    public OnDemandServices providerOf(ServiceState... served) {
        FakeServiceControl control = new FakeServiceControl();
        for (ServiceState state : served) {
            control.add(TASK, state);
        }
        TitanServiceControl.setControl(control);
        return new HolderOnDemandServices();
    }

    /**
     * Keeps rows per task the way the bridge reports them; a start adds a starting row, a stop
     * marks one stopped.
     */
    private static final class FakeServiceControl implements ServiceControl {

        private final Map<String, List<Map<String, Object>>> rows = new HashMap<>();
        private int started;

        void add(String task, ServiceState state) {
            this.rows.computeIfAbsent(task, key -> new ArrayList<>()).add(row(state));
        }

        @Override
        public List<Map<String, Object>> servicesByTask(String task) {
            return List.copyOf(this.rows.getOrDefault(task, List.of()));
        }

        @Override
        public boolean start(String task) {
            this.started++;
            add(task, new ServiceState(task + "-" + this.started, ServiceState.Phase.STARTING, 0, 0));
            return true;
        }

        @Override
        public boolean stop(String serviceName) {
            for (List<Map<String, Object>> listed : this.rows.values()) {
                for (int index = 0; index < listed.size(); index++) {
                    Map<String, Object> service = listed.get(index);
                    if (service.get(ServiceControl.NAME).equals(serviceName) && !"STOPPED".equals(service.get(ServiceControl.PHASE))) {
                        listed.set(index, row(new ServiceState(serviceName, ServiceState.Phase.STOPPED, (Integer) service.get(ServiceControl.ONLINE), (Integer) service.get(ServiceControl.MAX))));
                        return true;
                    }
                }
            }
            return false;
        }

        private static Map<String, Object> row(ServiceState state) {
            return Map.of(ServiceControl.NAME, state.name(), ServiceControl.PHASE, state.phase().name(), ServiceControl.ONLINE, state.online(), ServiceControl.MAX, state.max());
        }
    }
}
