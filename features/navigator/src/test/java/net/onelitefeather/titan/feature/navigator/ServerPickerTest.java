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

    @DisplayName("Only the minigames ElytraRace and Slender offer the server choice")
    @Test
    void onlyMinigamesOfferTheChoice() {
        assertEquals(java.util.Set.of(Destination.ELYTRA_RACE, Destination.SLENDER), ServerPicker.DESTINATIONS, "servers never open the picker");
    }

    @DisplayName("The picker lists the services sorted by name")
    @Test
    void entriesAreSortedByName() {
        List<ServiceCount> entries = ServerPicker.entries(List.of(TWO, ONE));

        assertEquals(List.of(ONE, TWO), entries, "ElytraRace-1 comes before ElytraRace-2");
    }

    @DisplayName("The picker holds at most six rows of services")
    @Test
    void entriesAreCappedAtSixRows() {
        List<ServiceCount> many = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            many.add(new ServiceCount("ElytraRace-" + String.format("%02d", i), 0, 50));
        }

        assertEquals(54, ServerPicker.entries(many).size(), "six rows of nine slots");
    }

    @DisplayName("The picker has one row per nine services, at least one and at most six")
    @Test
    void rowsFollowTheEntryCount() {
        assertEquals(1, ServerPicker.rows(2), "two services fit one row");
        assertEquals(1, ServerPicker.rows(9), "nine services fit one row");
        assertEquals(2, ServerPicker.rows(10), "ten services need two rows");
        assertEquals(6, ServerPicker.rows(54), "54 services fill six rows");
    }
}
