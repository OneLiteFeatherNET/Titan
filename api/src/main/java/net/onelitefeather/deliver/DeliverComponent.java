package net.onelitefeather.deliver;


import net.minestom.server.entity.Player;

import java.util.UUID;

public sealed interface DeliverComponent permits DeliverComponent.FleetDeliverComponent, DeliverComponent.GameServerDeliverComponent, FleetDeliverComponentImpl, GameServerDeliverComponentImpl {
    /**
     * The type of deliver component
     * @return the type
     */
    DeliverType type();

    UUID playerId();

    static FleetBuilder fleetBuilder() {
        return new FleetBuilderImpl();
    }

    static GameServerBuilder gameServerBuilder() {
        return new GameServerBuilderImpl();
    }

    static FleetDeliverComponent fleetFrom(byte[] data) {
        return FleetDeliverComponentImpl.from(data);
    }

    static GameServerDeliverComponent gameServerFrom(byte[] data) {
        return GameServerDeliverComponentImpl.from(data);
    }

    sealed interface FleetDeliverComponent extends DeliverComponent permits FleetDeliverComponentImpl {
        String fleetName();
    }

    sealed interface GameServerDeliverComponent extends DeliverComponent permits GameServerDeliverComponentImpl {
        String gameServer();
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
