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

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.onelitefeather.titan.core.portal.ServiceCount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TaskPlayersTest {

    @DisplayName("The players of a task are the sum over its running services")
    @Test
    void playersSumTheRunningServices() {
        List<ServiceCount> running = List.of(new ServiceCount("Survival-1", 3, 20), new ServiceCount("Survival-2", 4, 20));

        Assertions.assertEquals(OptionalInt.of(7), TaskPlayers.online(running), "3 and 4 players on two services make 7");
    }

    @DisplayName("A running task with nobody on it reports zero, not unknown")
    @Test
    void emptyRunningTaskReportsZero() {
        Assertions.assertEquals(OptionalInt.of(0), TaskPlayers.online(List.of(new ServiceCount("Survival-1", 0, 20))), "a service that runs with nobody on it is zero players");
    }

    @DisplayName("A task that runs nowhere has an unknown player count")
    @Test
    void nothingRunningIsUnknown() {
        Assertions.assertTrue(TaskPlayers.online(List.of()).isEmpty(), "no service means no count line");
    }

    @DisplayName("A task that runs nowhere is readable and lists no services")
    @Test
    void nothingRunningIsAnEmptyReading() {
        FakePlayerCounts counts = new FakePlayerCounts();

        Assertions.assertEquals(Optional.of(List.of()), new TaskPlayers(Optional.of(counts)).services("Survival"), "a read that finds nothing running is an empty list, not a failure");
    }

    @DisplayName("A running task lists its services")
    @Test
    void runningTaskListsItsServices() {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("Survival", new ServiceCount("Survival-1", 7, 20));

        Assertions.assertEquals(Optional.of(List.of(new ServiceCount("Survival-1", 7, 20))), new TaskPlayers(Optional.of(counts)).services("Survival"), "the services as the provider reports them");
    }

    @DisplayName("Without a provider the services of a task cannot be read")
    @Test
    void noProviderCannotReadServices() {
        Assertions.assertEquals(Optional.empty(), new TaskPlayers(Optional.empty()).services("Survival"), "nothing can be read without a provider, which is not the same as nothing running");
    }

    @DisplayName("A provider that cannot serve tasks leaves the services of a task unreadable")
    @Test
    void unsupportedTasksCannotReadServices() {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serve("Survival", 7);
        counts.doNotSupportTasks();

        Assertions.assertEquals(Optional.empty(), new TaskPlayers(Optional.of(counts)).services("Survival"), "a provider without task support cannot be asked, so nothing is read");
    }

    @DisplayName("A failing lookup reports the services as unreadable instead of throwing or looking empty")
    @Test
    void failingLookupIsUnreadable() {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.failWith(new IllegalStateException("bridge down"));

        Assertions.assertEquals(Optional.empty(), new TaskPlayers(Optional.of(counts)).services("Survival"), "a failed read must not look like a task that runs nowhere");
    }
}
