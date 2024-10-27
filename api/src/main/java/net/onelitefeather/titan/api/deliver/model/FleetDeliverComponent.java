package net.onelitefeather.titan.api.deliver.model;

import java.io.Serializable;
import java.util.UUID;

record FleetDeliverComponent(DeliverType type, String fleetName, UUID playerId) implements DeliverComponent, Serializable {
}
