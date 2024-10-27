package net.onelitefeather.titan.api.deliver.model;

import net.minestom.server.entity.Player;

import java.util.UUID;

public sealed interface DeliverComponent permits FleetDeliverComponent, GameServerDeliverComponent {
    DeliverType type();

    UUID playerId();

    static FleetBuilder fleetBuilder() {
        return new FleetBuilderImpl();
    }

    sealed interface Builder<T extends Builder<T>> {

        T playerId(UUID playerId);

        default T player(Player player) {
            return playerId(player.getUuid());
        }

        DeliverComponent build();
    }

    sealed interface FleetBuilder extends Builder<FleetBuilder> permits FleetBuilderImpl {
        FleetBuilder fleetName(String fleetName);
    }

    sealed interface GameServerBuilder extends Builder<GameServerBuilder> permits GameServerBuilderImpl {
        GameServerBuilder gameServerId(String gameServerId);
    }
}
