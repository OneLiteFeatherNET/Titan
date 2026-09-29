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
package net.onelitefeather.titan.feature.season;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.function.Supplier;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.ConnectionManager;

/** Default {@link OnlinePlayers}: the players in the play state. */
@Singleton
final class ConnectionOnlinePlayers implements OnlinePlayers {

    private final Supplier<ConnectionManager> connections;

    @Inject
    ConnectionOnlinePlayers() {
        // Looked up per call: the process may not exist yet when this bean is built.
        this(MinecraftServer::getConnectionManager);
    }

    ConnectionOnlinePlayers(Supplier<ConnectionManager> connections) {
        this.connections = connections;
    }

    @Override
    public int count() {
        return this.connections.get().getOnlinePlayerCount();
    }
}
