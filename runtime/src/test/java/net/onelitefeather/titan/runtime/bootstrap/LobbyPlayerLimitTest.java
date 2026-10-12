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
package net.onelitefeather.titan.runtime.bootstrap;

import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.ping.ServerListPingType;
import net.minestom.server.ping.Status;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class LobbyPlayerLimitTest {

    private static ServerListPingEvent ping() {
        return new ServerListPingEvent(ServerListPingType.MODERN_FULL_RGB);
    }

    @DisplayName("Minestom's own ping reports online + 1 as the maximum, which CloudNet would store as the limit")
    @Test
    void minestomDefaultReportsOnePlayerAboveOnline(Env env) {
        Status.PlayerInfo info = ping().getStatus().playerInfo();

        Assertions.assertEquals(info.onlinePlayers() + 1, info.maxPlayers(), "the default the CloudNet bridge read before this fix");
    }

    @DisplayName("The ping reports the configured limit and keeps the online count and description")
    @Test
    void pingReportsTheLimit(Env env) {
        ServerListPingEvent event = ping();
        Status before = event.getStatus();

        new LobbyPlayerLimit(100).report(event);

        Status.PlayerInfo info = event.getStatus().playerInfo();
        Assertions.assertEquals(100, info.maxPlayers(), "max players");
        Assertions.assertEquals(before.playerInfo().onlinePlayers(), info.onlinePlayers(), "online count untouched");
        Assertions.assertEquals(before.description(), event.getStatus().description(), "description untouched");
    }

    @DisplayName("A limit below 1 is rejected, naming the key")
    @Test
    void rejectsNonPositiveLimit() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> new LobbyPlayerLimit(0));

        Assertions.assertTrue(thrown.getMessage().contains(LobbyPlayerLimit.KEY), "message names the key: " + thrown.getMessage());
    }
}
