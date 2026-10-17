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
import net.onelitefeather.titan.core.portal.ServiceCount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuickJoinTest {

    private static Optional<String> pickedName(List<ServiceCount> running) {
        return QuickJoin.pick(running).map(ServiceCount::name);
    }

    @DisplayName("Quick join picks the ready, not full service with the most players")
    @Test
    void picksTheMostPopulatedReadyService() {
        List<ServiceCount> running = List.of(new ServiceCount("ElytraRace-1", 4, 20), new ServiceCount("ElytraRace-2", 12, 20), new ServiceCount("ElytraRace-3", 9, 20));
        Assertions.assertEquals(Optional.of("ElytraRace-2"), pickedName(running), "the most populated ready round is filled first");
    }

    @DisplayName("Quick join breaks a tie on the player count by the lowest name")
    @Test
    void tieGoesToTheLowestName() {
        List<ServiceCount> running = List.of(new ServiceCount("ElytraRace-2", 12, 20), new ServiceCount("ElytraRace-1", 12, 20));
        Assertions.assertEquals(Optional.of("ElytraRace-1"), pickedName(running), "a tie must pick the same service every time");
    }

    @DisplayName("Quick join skips a full service even when it has the most players")
    @Test
    void skipsFullServices() {
        List<ServiceCount> running = List.of(new ServiceCount("ElytraRace-1", 20, 20), new ServiceCount("ElytraRace-2", 5, 20));
        Assertions.assertEquals(Optional.of("ElytraRace-2"), pickedName(running), "a full service cannot be joined");
    }

    @DisplayName("Quick join skips a service that has not announced its limit")
    @Test
    void skipsNotReadyServices() {
        List<ServiceCount> running = List.of(new ServiceCount("ElytraRace-1", 15, 0), new ServiceCount("ElytraRace-2", 1, 20));
        Assertions.assertEquals(Optional.of("ElytraRace-2"), pickedName(running), "a service without a limit is not ready");
    }

    @DisplayName("Quick join finds nothing when every service is full or not ready")
    @Test
    void noReadyServiceIsEmpty() {
        List<ServiceCount> running = List.of(new ServiceCount("ElytraRace-1", 20, 20), new ServiceCount("ElytraRace-2", 3, 0));
        Assertions.assertEquals(Optional.empty(), pickedName(running), "without a joinable service the picker opens instead");
    }

    @DisplayName("Quick join finds nothing when no service runs")
    @Test
    void noServiceIsEmpty() {
        Assertions.assertEquals(Optional.empty(), pickedName(List.of()), "nothing running means nothing to join");
    }
}
