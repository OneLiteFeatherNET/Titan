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
import java.util.OptionalInt;
import net.onelitefeather.titan.core.portal.ServiceCount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TaskCapacityTest {

    @DisplayName("The capacity of a task is the sum of its services' limits when every one has announced a limit")
    @Test
    void sumsTheLimits() {
        List<ServiceCount> running = List.of(new ServiceCount("Survival-1", 3, 20), new ServiceCount("Survival-2", 4, 30));

        Assertions.assertEquals(OptionalInt.of(50), TaskPlayers.capacity(running), "20 and 30 players fit");
    }

    @DisplayName("The capacity is unknown while one service has not announced its limit")
    @Test
    void unknownWhileOneLimitIsMissing() {
        List<ServiceCount> running = List.of(new ServiceCount("Survival-1", 3, 20), new ServiceCount("Survival-2", 4, 0));

        Assertions.assertTrue(TaskPlayers.capacity(running).isEmpty(), "a partial sum would show a maximum that is too small");
    }

    @DisplayName("The capacity is unknown when nothing runs")
    @Test
    void unknownWithoutServices() {
        Assertions.assertTrue(TaskPlayers.capacity(List.of()).isEmpty(), "no service means no maximum to show");
    }
}
