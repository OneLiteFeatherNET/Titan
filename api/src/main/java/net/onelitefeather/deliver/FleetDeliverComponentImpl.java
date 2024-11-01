package net.onelitefeather.deliver;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.UUID;

record FleetDeliverComponentImpl(DeliverType type, String fleetName, UUID playerId) implements DeliverComponent, DeliverComponent.FleetDeliverComponent, Serializable {
    public static FleetDeliverComponent from(byte[] data) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return (FleetDeliverComponentImpl) objectInputStream.readObject();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize DeliverComponent", e);
        }
    }
}
