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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.onelitefeather.titan.core.service.OnDemandServices;
import net.onelitefeather.titan.core.service.ServiceState;
import net.onelitefeather.titan.core.service.ServiceState.Phase;
import org.junit.jupiter.api.Test;

/**
 * What every {@link OnDemandServices} must do. The fake the feature tests lean on and the holder
 * backed by the CloudNet bridge run the same cases, so the fake cannot drift from the real one.
 */
public interface OnDemandServicesContract {

    String TASK = "Slender-Staging";
    ServiceState READY = new ServiceState("Slender-Staging-1", Phase.RUNNING, 3, 20);
    ServiceState UNANNOUNCED = new ServiceState("Slender-Staging-2", Phase.RUNNING, 0, 0);
    ServiceState BOOTING = new ServiceState("Slender-Staging-3", Phase.STARTING, 0, 20);
    ServiceState DOWN = new ServiceState("Slender-Staging-4", Phase.STOPPED, 0, 20);

    /**
     * A fresh provider that reports exactly {@code served} as the services of {@link #TASK} and
     * nothing for any other task. Called once per test.
     */
    OnDemandServices providerOf(ServiceState... served);

    @Test
    default void start_acceptsTheFirstRequestAndCountsTheServiceAsStarting() {
        OnDemandServices services = providerOf();

        assertTrue(services.start(TASK), "the first request for a task without services is accepted");
        List<ServiceState> listed = services.services(TASK).orElseThrow();
        assertEquals(1, listed.size(), "the accepted start shows up as one service");
        assertEquals(Phase.STARTING, listed.getFirst().phase(), "a freshly started service is starting");
    }

    @Test
    default void ready_onlyForRunningWithAnAnnouncedLimit() {
        OnDemandServices services = providerOf(READY, UNANNOUNCED, BOOTING, DOWN);

        List<Boolean> ready = services.services(TASK).orElseThrow().stream().map(ServiceState::ready).toList();

        assertEquals(List.of(true, false, false, false), ready, "only a running service that announced a limit is ready");
    }

    @Test
    default void stop_isIdempotent() {
        OnDemandServices services = providerOf();
        services.start(TASK);
        String name = services.services(TASK).orElseThrow().getFirst().name();

        assertTrue(services.stop(name), "the first stop of a live service is accepted");
        assertFalse(services.stop(name), "a second stop of the same service is refused without an exception");
    }

    @Test
    default void services_ofAnUnknownTask_isAnEmptyList() {
        OnDemandServices services = providerOf(READY);

        assertEquals(Optional.of(List.of()), services.services("Bounce-Staging"), "a task that runs nowhere lists no services");
    }
}
