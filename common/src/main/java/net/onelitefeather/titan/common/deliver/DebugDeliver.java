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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.entity.Player;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Deliver} used when CloudNet is not available (standalone runs: local, tests, AOT
 * training). There is no CloudNet runtime to hand the request to - see
 * {@link TitanServerConnector} - so a click can never actually move the player to another
 * service. To keep a local-only run testable by hand, this reports the delivery it would have
 * made instead of silently doing nothing: it tells the clicking player, in chat, which task or
 * server they would have been sent to, and logs the same information for the operator.
 *
 * <p>Logs exactly one {@code INFO} line per call. That is acceptable here because this only runs
 * once per explicit player click in a local-only run, never once per tick.
 */
public final class DebugDeliver implements Deliver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebugDeliver.class);

    private static final String TASK_MESSAGE = "<prefix> <gray>No CloudNet running here - you would be sent to task <white><target>";
    private static final String SERVER_MESSAGE = "<prefix> <gray>No CloudNet running here - you would be sent to server <white><target>";

    @Override
    public void sendPlayer(Player player, DeliverComponent component) {
        if (player == null)
            return;
        if (component == null)
            return;

        switch (component) {
            case DeliverComponent.TaskComponent taskComponent ->
                report(player, "task", taskComponent.taskName(), TASK_MESSAGE);
            case DeliverComponent.ServerDeliverComponent serverDeliverComponent ->
                report(player, "server", serverDeliverComponent.gameServer(), SERVER_MESSAGE);
        }
    }

    private static void report(Player player, String kind, String target, String messageTemplate) {
        Component message = MiniMessage.miniMessage().deserialize(messageTemplate, Placeholder.unparsed("target", target));
        player.sendMessage(message);
        LOGGER.info("Debug deliver: would send {} ({}) to {} {}", player.getUsername(), player.getUuid(), kind, target);
    }
}
