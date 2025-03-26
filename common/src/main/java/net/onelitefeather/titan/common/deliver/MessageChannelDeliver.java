package net.onelitefeather.titan.common.deliver;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import net.minestom.server.entity.Player;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;

public final class MessageChannelDeliver implements Deliver {

    private static final ServiceRegistry serviceRegistry = InjectionLayer.ext().instance(ServiceRegistry.class);
    private static final PlayerManager playerManager = serviceRegistry.firstProvider(PlayerManager.class);

    @Override
    public void sendPlayer(Player player, DeliverComponent component) {
        if (player == null) throw new IllegalArgumentException("player cannot be null");
        if (component == null) throw new IllegalArgumentException("component cannot be null");
        switch (component) {
            case DeliverComponent.TaskComponent taskComponent -> playerManager.playerExecutor(player.getUuid()).connectToTask(taskComponent.taskName(), ServerSelectorType.LOWEST_PLAYERS);
            case DeliverComponent.ServerDeliverComponent serverDeliverComponent -> playerManager.playerExecutor(player.getUuid()).connect(serverDeliverComponent.gameServer());
            case null, default -> throw new IllegalStateException("Unexpected value: " + component.type());
        };

    }
}
