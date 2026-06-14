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

import java.util.UUID;

/**
 * Cross-classloader bridge for connecting players to other CloudNet services.
 *
 * <p>The CloudNet bridge (with its {@code PlayerManager}) runs inside the bridge extension
 * classloader; the application cannot reference those classes. This holder lives on the shared
 * application classloader and lets the application request a server switch via JDK types only.
 * The bridge extension installs the actual {@link ServerConnector}; until then calls are no-ops.
 */
public final class TitanServerConnector {

    private static volatile ServerConnector connector;

    private TitanServerConnector() {
    }

    /**
     * Installs the connector. Called by the bridge extension once the bridge is up.
     *
     * @param serverConnector the connector backed by the CloudNet bridge player manager
     */
    public static void setConnector(ServerConnector serverConnector) {
        connector = serverConnector;
    }

    /**
     * Connects the player to the best service of the given task, or does nothing if no connector
     * has been installed (for example when running standalone).
     *
     * @param playerId the player's unique id
     * @param taskName the CloudNet task to connect to
     */
    public static void connectToTask(UUID playerId, String taskName) {
        ServerConnector current = connector;
        if (current != null) {
            current.connectToTask(playerId, taskName);
        }
    }

    /**
     * Connects the player to a specific service, or does nothing if no connector has been
     * installed.
     *
     * @param playerId    the player's unique id
     * @param serviceName the CloudNet service to connect to
     */
    public static void connectToServer(UUID playerId, String serviceName) {
        ServerConnector current = connector;
        if (current != null) {
            current.connectToServer(playerId, serviceName);
        }
    }
}
