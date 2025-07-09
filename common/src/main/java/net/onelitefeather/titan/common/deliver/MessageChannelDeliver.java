/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.common.deliver;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import net.minestom.server.entity.Player;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;

public final class MessageChannelDeliver implements Deliver {

    @Override
    public void sendPlayer(Player player, DeliverComponent component) {
        if (player == null) return;
        if (component == null) return;

        var serviceRegistry = InjectionLayer.ext().instance(ServiceRegistry.class);
        if (serviceRegistry == null) return;

        var playerManager = serviceRegistry.registration(PlayerManager.class, "playerManager");
        if (playerManager == null) return;

        var executor = playerManager.serviceInstance().playerExecutor(player.getUuid());
        switch (component) {
            case DeliverComponent.TaskComponent taskComponent -> executor.connectToTask(taskComponent.taskName(), ServerSelectorType.LOWEST_PLAYERS);
            case DeliverComponent.ServerDeliverComponent serverDeliverComponent -> executor.connect(serverDeliverComponent.gameServer());
            case null, default -> throw new IllegalStateException("Unexpected value: " + component.type());
        };

    }
}
