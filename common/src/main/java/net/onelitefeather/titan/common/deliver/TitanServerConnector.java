/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
