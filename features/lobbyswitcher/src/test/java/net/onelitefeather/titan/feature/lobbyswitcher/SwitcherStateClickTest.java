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

class SwitcherStateClickTest {

    private static final LobbyIdentity OWN = new LobbyIdentity("Lobby", "Lobby-1");

    private static SwitcherState decide(String target, ServiceCount... fresh) {
        return SwitcherState.ofClick(target, OWN, () -> List.of(fresh));
    }

    @DisplayName("A joinable target is sent")
    @Test
    void joinableIsSent() {
        Assertions.assertEquals(SwitcherState.JOINABLE, decide("Lobby-2", new ServiceCount("Lobby-2", 3, 50)));
    }

    @DisplayName("A target that filled up since the list was drawn is full")
    @Test
    void filledUpIsFull() {
        Assertions.assertEquals(SwitcherState.FULL, decide("Lobby-2", new ServiceCount("Lobby-2", 50, 50)));
    }

    @DisplayName("A target without a player limit is not ready")
    @Test
    void noLimitIsNotReady() {
        Assertions.assertEquals(SwitcherState.NOT_READY, decide("Lobby-2", new ServiceCount("Lobby-2", 0, 0)));
    }

    @DisplayName("A target that is no longer listed is gone")
    @Test
    void unlistedIsGone() {
        Assertions.assertEquals(SwitcherState.GONE, decide("Lobby-2", new ServiceCount("Lobby-3", 3, 50)));
    }

    @DisplayName("A target with no running service at all is gone")
    @Test
    void emptyListIsGone() {
        Assertions.assertEquals(SwitcherState.GONE, decide("Lobby-2"));
    }

    @DisplayName("The own lobby is current, even when it is full")
    @Test
    void ownLobbyIsCurrent() {
        Assertions.assertEquals(SwitcherState.CURRENT, decide("Lobby-1", new ServiceCount("Lobby-1", 50, 50)));
    }

    @DisplayName("The own lobby is current, even when the list does not show it")
    @Test
    void ownLobbyIsCurrentWhenUnlisted() {
        Assertions.assertEquals(SwitcherState.CURRENT, decide("Lobby-1"));
    }

    @DisplayName("A check that threw decides error")
    @Test
    void thrownCheckIsError() {
        SwitcherState decision = SwitcherState.ofClick("Lobby-2", OWN, () -> {
            throw new IllegalStateException("bridge down");
        });

        Assertions.assertEquals(SwitcherState.ERROR, decision);
    }

    @DisplayName("A check that returned decides from its result")
    @Test
    void returnedCheckDecides() {
        SwitcherState decision = SwitcherState.ofClick("Lobby-2", OWN, () -> List.of(new ServiceCount("Lobby-2", 1, 50)));

        Assertions.assertEquals(SwitcherState.JOINABLE, decision);
    }

    @DisplayName("The result keys are the telemetry values")
    @Test
    void resultKeys() {
        Assertions.assertEquals("sent", SwitcherState.JOINABLE.result());
        Assertions.assertEquals("current", SwitcherState.CURRENT.result());
        Assertions.assertEquals("full", SwitcherState.FULL.result());
        Assertions.assertEquals("not_ready", SwitcherState.NOT_READY.result());
        Assertions.assertEquals("gone", SwitcherState.GONE.result());
        Assertions.assertEquals("error", SwitcherState.ERROR.result());
    }
}
