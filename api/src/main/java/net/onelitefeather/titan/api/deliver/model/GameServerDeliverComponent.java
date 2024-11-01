package net.onelitefeather.titan.api.deliver.model;

import java.io.Serializable;
import java.util.UUID;

record GameServerDeliverComponent(DeliverType type, String gameServer, UUID playerId) implements DeliverComponent, Serializable {

}
