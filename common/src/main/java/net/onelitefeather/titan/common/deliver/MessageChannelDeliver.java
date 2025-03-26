package net.onelitefeather.titan.common.deliver;

import eu.cloudnetservice.common.concurrent.TaskUtil;
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

        var playerManager = serviceRegistry.firstProvider(PlayerManager.class);
        if (playerManager == null) return;

        var executor = playerManager.playerExecutor(player.getUuid());
        switch (component) {
            case DeliverComponent.TaskComponent taskComponent -> executor.connectToTask(taskComponent.taskName(), ServerSelectorType.LOWEST_PLAYERS);
            case DeliverComponent.ServerDeliverComponent serverDeliverComponent -> executor.connect(serverDeliverComponent.gameServer());
            case null, default -> throw new IllegalStateException("Unexpected value: " + component.type());
        };

    }
}
