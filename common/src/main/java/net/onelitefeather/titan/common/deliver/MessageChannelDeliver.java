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

import net.minestom.server.entity.Player;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;

/**
 * Sends a player to another CloudNet service. The actual switch runs in the CloudNet bridge
 * extension realm (the bridge {@code PlayerManager} is not on the application classpath); this
 * class only forwards the request through {@link TitanServerConnector} using JDK types.
 */
public final class MessageChannelDeliver implements Deliver {

    @Override
    public void sendPlayer(Player player, DeliverComponent component) {
        if (player == null)
            return;
        if (component == null)
            return;

        switch (component) {
            case DeliverComponent.TaskComponent taskComponent ->
                TitanServerConnector.connectToTask(player.getUuid(), taskComponent.taskName());
            case DeliverComponent.ServerDeliverComponent serverDeliverComponent ->
                TitanServerConnector.connectToServer(player.getUuid(), serverDeliverComponent.gameServer());
            case null, default ->
                throw new IllegalStateException("Unexpected value: " + component.type());
        }
    }
}
