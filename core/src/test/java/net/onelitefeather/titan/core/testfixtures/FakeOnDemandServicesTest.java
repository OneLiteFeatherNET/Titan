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
import org.junit.jupiter.api.Test;

class FakeOnDemandServicesTest implements OnDemandServicesContract {

    @Override
    public OnDemandServices providerOf(ServiceState... served) {
        return new FakeOnDemandServices().serve(TASK, served);
    }

    @Test
    void failReads_makesEveryReadEmpty() {
        FakeOnDemandServices services = new FakeOnDemandServices().serve(TASK, READY).failReads(true);

        assertEquals(Optional.empty(), services.services(TASK), "a read error is an empty Optional, not an empty list");
    }

    @Test
    void failReads_offRestoresTheServedServices() {
        FakeOnDemandServices services = new FakeOnDemandServices().serve(TASK, READY).failReads(true).failReads(false);

        assertEquals(Optional.of(List.of(READY)), services.services(TASK), "switching the failure off serves the services again");
    }

    @Test
    void rejectStarts_refusesAndAddsNothing() {
        FakeOnDemandServices services = new FakeOnDemandServices().rejectStarts(true);

        assertFalse(services.start(TASK), "a rejected start is refused");
        assertEquals(Optional.of(List.of()), services.services(TASK), "a rejected start adds no service");
    }

    @Test
    void starts_countsAcceptedAndRejectedCalls() {
        FakeOnDemandServices services = new FakeOnDemandServices().rejectStarts(true);
        services.start(TASK);
        services.rejectStarts(false);
        services.start(TASK);

        assertEquals(2, services.starts(), "a rejected call counts as a start call too");
    }

    @Test
    void reads_countsEveryServicesCall() {
        FakeOnDemandServices services = new FakeOnDemandServices().serve(TASK, READY);
        services.services(TASK);
        services.failReads(true);
        services.services(TASK);

        assertEquals(2, services.reads(), "a failed read is still a read call");
    }

    @Test
    void stopped_listsTheAcceptedStopsInOrder() {
        FakeOnDemandServices services = new FakeOnDemandServices();
        services.start(TASK);
        services.start(TASK);
        services.stop(TASK + "-2");
        services.stop(TASK + "-1");
        services.stop(TASK + "-2");

        assertEquals(List.of(TASK + "-2", TASK + "-1"), services.stopped(), "only accepted stops, in the order they were accepted");
    }

    @Test
    void stopped_isEmptyBeforeAnyStop() {
        assertTrue(new FakeOnDemandServices().stopped().isEmpty(), "nothing was stopped yet");
    }
}
