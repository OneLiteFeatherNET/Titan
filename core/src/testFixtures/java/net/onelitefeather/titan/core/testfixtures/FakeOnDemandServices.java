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
package net.onelitefeather.titan.core.testfixtures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.onelitefeather.titan.core.service.OnDemandServices;
import net.onelitefeather.titan.core.service.ServiceState;
import net.onelitefeather.titan.core.service.ServiceState.Phase;

/**
 * A test-only {@link OnDemandServices} that is not CloudNet: it keeps its services in memory. A
 * started service is {@link Phase#STARTING} until a test serves it as running; a stopped service
 * stays listed as {@link Phase#STOPPED}.
 */
public final class FakeOnDemandServices implements OnDemandServices {

    private final Map<String, List<ServiceState>> services = new HashMap<>();
    private int started;

    /** Replaces the services of {@code task} with {@code states}. */
    public FakeOnDemandServices serve(String task, ServiceState... states) {
        this.services.put(task, new ArrayList<>(List.of(states)));
        return this;
    }

    @Override
    public Optional<List<ServiceState>> services(String task) {
        return Optional.of(List.copyOf(this.services.getOrDefault(task, List.of())));
    }

    @Override
    public boolean start(String task) {
        this.started++;
        this.services.computeIfAbsent(task, key -> new ArrayList<>()).add(new ServiceState(task + "-" + this.started, Phase.STARTING, 0, 0));
        return true;
    }

    @Override
    public boolean stop(String serviceName) {
        for (List<ServiceState> listed : this.services.values()) {
            for (int index = 0; index < listed.size(); index++) {
                ServiceState state = listed.get(index);
                if (state.name().equals(serviceName) && state.phase() != Phase.STOPPED) {
                    listed.set(index, new ServiceState(state.name(), Phase.STOPPED, state.online(), state.max()));
                    return true;
                }
            }
        }
        return false;
    }
}
