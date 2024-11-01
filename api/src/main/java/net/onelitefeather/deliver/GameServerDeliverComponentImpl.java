package net.onelitefeather.deliver;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.UUID;

record GameServerDeliverComponentImpl(DeliverType type, String gameServer, UUID playerId) implements DeliverComponent, DeliverComponent.GameServerDeliverComponent, Serializable {

    public static GameServerDeliverComponent from(byte[] data) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return (GameServerDeliverComponentImpl) objectInputStream.readObject();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize DeliverComponent", e);
        }
    }
}
