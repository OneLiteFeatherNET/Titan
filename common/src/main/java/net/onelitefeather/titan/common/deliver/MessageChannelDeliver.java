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
import net.onelitefeather.titan.api.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends a player to another CloudNet service. The actual switch runs in the CloudNet bridge
 * extension realm (the bridge {@code PlayerManager} is not on the application classpath); this
 * class only forwards the request through {@link TitanServerConnector} using JDK types.
 */
public final class MessageChannelDeliver implements Deliver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageChannelDeliver.class);

    @Override
    public void sendPlayer(Player player, DeliverComponent component) {
        if (player == null)
            return;
        if (component == null)
            return;

        switch (component) {
            case DeliverComponent.TaskComponent taskComponent -> {
                if (!TitanServerConnector.connectToTask(player.getUuid(), taskComponent.taskName())) {
                    warnMissingConnector(player, "task", taskComponent.taskName());
                }
            }
            case DeliverComponent.ServerDeliverComponent serverDeliverComponent -> {
                if (!TitanServerConnector.connectToServer(player.getUuid(), serverDeliverComponent.gameServer())) {
                    warnMissingConnector(player, "server", serverDeliverComponent.gameServer());
                }
            }
            case null, default ->
                throw new IllegalStateException("Unexpected value: " + component.type());
        }
    }

    // Without a connector the click silently strands the player; the log is the only trace of it.
    private static void warnMissingConnector(Player player, String kind, String target) {
        LOGGER.warn("Server connector missing: cannot send {} ({}) to {} {}", player.getUsername(), player.getUuid(), kind, target);
    }
}
