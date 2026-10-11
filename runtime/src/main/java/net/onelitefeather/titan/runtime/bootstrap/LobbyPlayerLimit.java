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

import io.avaje.config.Config;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.ping.Status;

/**
 * The player limit the lobby reports in the server list ping and to CloudNet.
 *
 * @param maxPlayers the configured limit, at least 1
 */
public record LobbyPlayerLimit(int maxPlayers) {

    static final String KEY = "lobby.maxPlayers";

    public LobbyPlayerLimit {
        if (maxPlayers < 1) {
            throw new IllegalArgumentException(KEY + " must be at least 1, was " + maxPlayers);
        }
    }

    /**
     * Aborts startup, naming the key, when the configured value is not a whole number of at least
     * 1.
     */
    static LobbyPlayerLimit read() {
        return new LobbyPlayerLimit(Config.getInt(KEY));
    }

    /** Replaces Minestom's default maximum (online + 1); online count and description stay. */
    void report(ServerListPingEvent event) {
        Status status = event.getStatus();
        Status.PlayerInfo info = status.playerInfo();
        if (info == null) {
            return;
        }
        Status.PlayerInfo limited = new Status.PlayerInfo(info.onlinePlayers(), this.maxPlayers, info.sample());
        event.setStatus(Status.builder(status).playerInfo(limited).build());
    }
}
