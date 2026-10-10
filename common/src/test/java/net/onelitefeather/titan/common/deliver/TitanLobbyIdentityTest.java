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
package net.onelitefeather.titan.common.deliver;

import java.util.Optional;
import net.onelitefeather.titan.core.lobby.LobbyIdentity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitanLobbyIdentityTest {

    // The holder is static by nature; every test leaves it empty again.
    @AfterEach
    void emptyHolder() {
        TitanLobbyIdentity.set(null);
    }

    @DisplayName("An unset holder knows no identity")
    @Test
    void unsetIsEmpty() {
        assertTrue(TitanLobbyIdentity.self().isEmpty(), "nothing installed");
    }

    @DisplayName("An installed identity is returned")
    @Test
    void installedIdentityIsReturned() {
        LobbyIdentity identity = new LobbyIdentity("Lobby", "Lobby-1");

        TitanLobbyIdentity.set(identity);

        assertEquals(Optional.of(identity), TitanLobbyIdentity.self(), "the installed identity");
    }

    @DisplayName("Setting null clears the identity again")
    @Test
    void nullClears() {
        TitanLobbyIdentity.set(new LobbyIdentity("Lobby", "Lobby-1"));

        TitanLobbyIdentity.set(null);

        assertTrue(TitanLobbyIdentity.self().isEmpty(), "cleared");
    }

    @DisplayName("The holder works as the application's LobbyIdentities")
    @Test
    void holderIsLobbyIdentities() {
        TitanLobbyIdentity.set(new LobbyIdentity("Lobby", "Lobby-2"));

        assertEquals("Lobby-2", TitanLobbyIdentity.identities().self().orElseThrow().serviceName(), "via the interface");
    }
}
