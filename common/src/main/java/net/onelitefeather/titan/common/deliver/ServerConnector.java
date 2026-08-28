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
 * Connects a player to another CloudNet service. Implemented in the CloudNet bridge extension
 * realm (where the bridge {@code PlayerManager} is visible) and invoked from the application via
 * {@link TitanServerConnector}. Only JDK types cross the classloader boundary.
 */
public interface ServerConnector {

    /**
     * Connects the player to the best service of the given task.
     *
     * @param playerId the player's unique id
     * @param taskName the CloudNet task to connect to
     */
    void connectToTask(UUID playerId, String taskName);

    /**
     * Connects the player to a specific service.
     *
     * @param playerId    the player's unique id
     * @param serviceName the CloudNet service to connect to
     */
    void connectToServer(UUID playerId, String serviceName);
}
