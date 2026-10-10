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
package net.onelitefeather.titan.core.lobby;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyIdentityTest {

    @DisplayName("The default identities know no own lobby")
    @Test
    void unknownWithoutBridge() {
        assertTrue(LobbyIdentities.none().self().isEmpty(), "no bridge, no identity");
    }

    @DisplayName("A blank task is rejected")
    @Test
    void blankTaskIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LobbyIdentity(" ", "Lobby-1"), "blank task");
    }

    @DisplayName("A blank service name is rejected")
    @Test
    void blankServiceNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LobbyIdentity("Lobby", ""), "blank service name");
    }

    @DisplayName("A valid identity keeps its values")
    @Test
    void keepsValues() {
        LobbyIdentity identity = new LobbyIdentity("Lobby", "Lobby-1");

        assertEquals("Lobby", identity.task(), "task");
        assertEquals("Lobby-1", identity.serviceName(), "service name");
    }
}
