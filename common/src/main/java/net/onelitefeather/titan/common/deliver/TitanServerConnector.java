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
 * Cross-classloader bridge for connecting players to other CloudNet services. The bridge extension
 * (with its {@code PlayerManager}) runs in its own classloader, unreachable from the application,
 * so this holder lives on the shared application classloader and exchanges only JDK types; until
 * the bridge installs a {@link ServerConnector}, calls report {@code false} and do nothing.
 */
public final class TitanServerConnector {

    private static volatile ServerConnector connector;

    private TitanServerConnector() {
    }

    public static void setConnector(ServerConnector serverConnector) {
        connector = serverConnector;
    }

    public static boolean isInstalled() {
        return connector != null;
    }

    public static boolean connectToTask(UUID playerId, String taskName) {
        ServerConnector current = connector;
        if (current == null) {
            return false;
        }
        current.connectToTask(playerId, taskName);
        return true;
    }

    public static boolean connectToServer(UUID playerId, String serviceName) {
        ServerConnector current = connector;
        if (current == null) {
            return false;
        }
        current.connectToServer(playerId, serviceName);
        return true;
    }
}
