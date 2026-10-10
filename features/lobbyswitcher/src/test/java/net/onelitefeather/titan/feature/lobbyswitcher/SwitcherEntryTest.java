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
package net.onelitefeather.titan.feature.lobbyswitcher;

import java.util.List;
import net.onelitefeather.titan.core.lobby.LobbyIdentity;
import net.onelitefeather.titan.core.portal.ServiceCount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SwitcherEntryTest {

    private static final LobbyIdentity OWN = new LobbyIdentity("Lobby", "Lobby-1");

    private static SwitcherState stateOf(String name, int online, int max) {
        return SwitcherEntry.of(new ServiceCount(name, online, max), OWN).state();
    }

    @DisplayName("The own service is CURRENT")
    @Test
    void ownServiceIsCurrent() {
        Assertions.assertEquals(SwitcherState.CURRENT, stateOf("Lobby-1", 3, 50));
    }

    @DisplayName("The own service is CURRENT even when it is full")
    @Test
    void ownServiceWinsOverFull() {
        Assertions.assertEquals(SwitcherState.CURRENT, stateOf("Lobby-1", 50, 50));
    }

    @DisplayName("online equal to max is FULL")
    @Test
    void onlineAtMaxIsFull() {
        Assertions.assertEquals(SwitcherState.FULL, stateOf("Lobby-2", 50, 50));
    }

    @DisplayName("online above max is FULL")
    @Test
    void onlineAboveMaxIsFull() {
        Assertions.assertEquals(SwitcherState.FULL, stateOf("Lobby-2", 51, 50));
    }

    @DisplayName("max of 0 is NOT_READY, even with players online")
    @Test
    void zeroMaxIsNotReady() {
        Assertions.assertEquals(SwitcherState.NOT_READY, stateOf("Lobby-2", 0, 0));
        Assertions.assertEquals(SwitcherState.NOT_READY, stateOf("Lobby-2", 4, 0));
    }

    @DisplayName("online below max is JOINABLE")
    @Test
    void belowMaxIsJoinable() {
        Assertions.assertEquals(SwitcherState.JOINABLE, stateOf("Lobby-2", 49, 50));
    }

    @DisplayName("An empty service with a limit is JOINABLE")
    @Test
    void emptyWithLimitIsJoinable() {
        Assertions.assertEquals(SwitcherState.JOINABLE, stateOf("Lobby-2", 0, 50));
    }

    @DisplayName("The entry keeps name and counts")
    @Test
    void entryKeepsCounts() {
        SwitcherEntry entry = SwitcherEntry.of(new ServiceCount("Lobby-2", 7, 50), OWN);

        Assertions.assertEquals(new SwitcherEntry("Lobby-2", 7, 50, SwitcherState.JOINABLE), entry);
    }

    @DisplayName("Entries are sorted by service name ascending, regardless of state")
    @Test
    void sortedByName() {
        List<SwitcherEntry> entries = SwitcherEntry.sorted(List.of(
                new ServiceCount("Lobby-3", 50, 50), new ServiceCount("Lobby-1", 2, 50), new ServiceCount("Lobby-2", 0, 0)), OWN);

        Assertions.assertEquals(List.of("Lobby-1", "Lobby-2", "Lobby-3"), entries.stream().map(SwitcherEntry::name).toList());
    }
}
