package net.onelitefeather.deliver;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.UUID;

record ServerDeliverComponentImpl(DeliverType type, String gameServer, UUID playerId) implements DeliverComponent, DeliverComponent.ServerDeliverComponent, Serializable {
}
