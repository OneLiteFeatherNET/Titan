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
package net.onelitefeather.titan.feature.navigator;

import java.util.Optional;
import net.onelitefeather.titan.core.portal.PlayerCount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TaskPlayersTest {

    @DisplayName("A running task reports its players and their max")
    @Test
    void runningTaskReportsOnline() {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serve("Survival", 7);

        Assertions.assertEquals(Optional.of(new PlayerCount(7, 100, true)), new TaskPlayers(Optional.of(counts)).count("Survival"));
    }

    @DisplayName("A running task with nobody on it reports zero, not unknown")
    @Test
    void emptyRunningTaskReportsZero() {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serve("Survival", 0);

        Assertions.assertEquals(Optional.of(new PlayerCount(0, 100, true)), new TaskPlayers(Optional.of(counts)).count("Survival"));
    }

    @DisplayName("A task that is not running is unknown")
    @Test
    void notRunningTaskIsUnknown() {
        Assertions.assertTrue(new TaskPlayers(Optional.of(new FakePlayerCounts())).count("Survival").isEmpty());
    }

    @DisplayName("Without a provider every task is unknown")
    @Test
    void noProviderIsUnknown() {
        Assertions.assertTrue(new TaskPlayers(Optional.empty()).count("Survival").isEmpty());
    }

    @DisplayName("A provider that cannot serve tasks is unknown")
    @Test
    void unsupportedTasksAreUnknown() {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serve("Survival", 7);
        counts.doNotSupportTasks();

        Assertions.assertTrue(new TaskPlayers(Optional.of(counts)).count("Survival").isEmpty());
    }

    @DisplayName("A failing lookup is unknown instead of throwing")
    @Test
    void failingLookupIsUnknown() {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.failWith(new IllegalStateException("bridge down"));

        Assertions.assertTrue(new TaskPlayers(Optional.of(counts)).count("Survival").isEmpty());
    }
}
