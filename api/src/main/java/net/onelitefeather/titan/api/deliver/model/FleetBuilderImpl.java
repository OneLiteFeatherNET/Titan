package net.onelitefeather.titan.api.deliver.model;

import java.util.UUID;

final class FleetBuilderImpl implements DeliverComponent.FleetBuilder {

    private UUID playerId;
    private String fleetName;

    @Override
    public FleetBuilderImpl fleetName(String fleetName) {
        this.fleetName = fleetName;
        return this;
    }

    @Override
    public FleetBuilderImpl playerId(UUID playerId) {
        this.playerId = playerId;
        return this;
    }

    @Override
    public DeliverComponent build() {
        if (playerId == null) {
            throw new IllegalStateException("Player ID not set");
        }
        if (fleetName == null) {
            throw new IllegalStateException("Fleet name not set");
        }
        return new FleetDeliverComponent(DeliverType.FLEET, fleetName, playerId);
    }
}
