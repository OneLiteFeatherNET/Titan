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
import net.onelitefeather.titan.core.service.ServiceState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The holder's own behaviour: no CloudNet installed, and the phase names of the bridge rows. */
class HolderOnDemandServicesTest {

    private static final String TASK = "Slender-Staging";

    @AfterEach
    void emptyHolder() {
        TitanServiceControl.setControl(null);
    }

    @DisplayName("Without a CloudNet control nothing can be read and every request is refused")
    @Test
    void refusesEverythingWithoutControl() {
        HolderOnDemandServices services = new HolderOnDemandServices();

        Assertions.assertEquals(Optional.empty(), services.services(TASK), "an unreadable task is an empty Optional");
        Assertions.assertFalse(services.start(TASK), "no control means no start");
        Assertions.assertFalse(services.stop("Slender-Staging-1"), "no control means no stop");
    }

    @DisplayName("An unknown phase name counts as starting, so it is never ready")
    @Test
    void unknownPhaseCountsAsStarting() {
        TitanServiceControl.setControl(new ControlWithRow(Map.of(ServiceControl.NAME, "Slender-Staging-1", ServiceControl.PHASE, "PAUSED", ServiceControl.ONLINE, 0, ServiceControl.MAX, 20)));

        ServiceState state = new HolderOnDemandServices().services(TASK).orElseThrow().getFirst();

        Assertions.assertEquals(ServiceState.Phase.STARTING, state.phase(), "an unknown phase is starting");
        Assertions.assertFalse(state.ready(), "a starting service is not ready");
    }

    /** Serves one row for every task and refuses to start or stop anything. */
    private record ControlWithRow(Map<String, Object> row) implements ServiceControl {

        @Override
        public List<Map<String, Object>> servicesByTask(String task) {
            return List.of(row);
        }

        @Override
        public boolean start(String task) {
            return false;
        }

        @Override
        public boolean stop(String serviceName) {
            return false;
        }
    }
}
