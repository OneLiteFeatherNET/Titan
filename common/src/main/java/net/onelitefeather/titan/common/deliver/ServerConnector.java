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
