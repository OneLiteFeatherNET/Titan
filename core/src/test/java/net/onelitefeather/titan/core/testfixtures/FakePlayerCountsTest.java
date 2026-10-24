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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.portal.SourceType;
import org.junit.jupiter.api.Test;

class FakePlayerCountsTest implements PlayerCountsContract {

    @Override
    public PlayerCounts countsOf(SourceType type, String name, ServiceCount... running) {
        return new FakePlayerCounts().serveServices(name, running);
    }

    @Test
    void set_pinsTheCountWhateverServicesAreServed() {
        FakePlayerCounts counts = new FakePlayerCounts().serveServices("Lobby", new ServiceCount("Lobby-1", 1, 2)).set(SourceType.TASK, "Lobby", new PlayerCount(9, 9, true));

        assertEquals(new PlayerCount(9, 9, true), counts.count(SourceType.TASK, "Lobby"), "the pinned count wins over the served services");
    }

    @Test
    void serve_isOneServiceNamedAfterTheTask() {
        FakePlayerCounts counts = new FakePlayerCounts().serve("Slender", 4);

        assertEquals(new ServiceCount("Slender-1", 4, 100), counts.running(SourceType.TASK, "Slender").getFirst(), "a single service with the given players");
    }

    @Test
    void reads_countsCountAndRunning() {
        FakePlayerCounts counts = new FakePlayerCounts();

        counts.count(SourceType.TASK, "Lobby");
        counts.running(SourceType.TASK, "Lobby");

        assertEquals(2, counts.reads(), "one read per call");
    }

    @Test
    void failWith_makesEveryReadThrow() {
        IllegalStateException failure = new IllegalStateException("provider down");
        FakePlayerCounts counts = new FakePlayerCounts().failWith(failure);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> counts.running(SourceType.TASK, "Lobby"), "running throws"));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> counts.count(SourceType.TASK, "Lobby"), "count throws"));
    }

    @Test
    void doNotSupport_dropsOnlyThatType() {
        FakePlayerCounts counts = new FakePlayerCounts().doNotSupport(SourceType.TASK);

        assertFalse(counts.supports(SourceType.TASK), "the dropped type");
        assertTrue(counts.supports(SourceType.GROUP), "every other type stays supported");
    }
}
