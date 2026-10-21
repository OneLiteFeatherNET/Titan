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

import java.util.ArrayList;
import java.util.List;
import net.onelitefeather.titan.core.portal.ServiceCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerPickerTest {

    private static final ServiceCount ONE = new ServiceCount("ElytraRace-1", 1, 50);
    private static final ServiceCount TWO = new ServiceCount("ElytraRace-2", 2, 50);

    @DisplayName("Only the minigames ElytraRace, Slender and Bounce offer the server choice")
    @Test
    void onlyMinigamesOfferTheChoice() {
        assertEquals(java.util.Set.of(Destination.ELYTRA_RACE, Destination.SLENDER, Destination.BOUNCE), ServerPicker.DESTINATIONS, "servers never open the picker");
    }

    @DisplayName("The picker lists the services in the order of its sort, by name first")
    @Test
    void entriesFollowTheSort() {
        assertEquals(List.of(ONE, TWO), ServerPicker.entries(List.of(TWO, ONE), PickerSort.NAME), "ElytraRace-1 comes before ElytraRace-2");
        assertEquals(List.of(TWO, ONE), ServerPicker.entries(List.of(ONE, TWO), PickerSort.MOST_PLAYERS), "the service with more players comes first");
    }

    @DisplayName("The picker keeps every running service, however many there are")
    @Test
    void entriesAreNotCapped() {
        List<ServiceCount> many = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            many.add(new ServiceCount("ElytraRace-" + String.format("%03d", i), 0, 50));
        }

        assertEquals(200, ServerPicker.entries(many, PickerSort.NAME).size(), "no service is dropped");
    }
}
