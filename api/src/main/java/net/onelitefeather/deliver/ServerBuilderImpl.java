package net.onelitefeather.deliver;

import java.util.UUID;

final class ServerBuilderImpl implements DeliverComponent.ServerBuilder {

    private String serverName;
    private UUID playerId;

    @Override
    public DeliverComponent.ServerBuilder serverName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    @Override
    public DeliverComponent.ServerBuilder playerId(UUID playerId) {
        this.playerId = playerId;
        return this;
    }

    @Override
    public DeliverComponent build() {
        if (serverName == null) {
            throw new IllegalArgumentException("Server cannot be null");
        }
        if (playerId == null) {
            throw new IllegalArgumentException("playerId cannot be null");
        }
        return new ServerDeliverComponentImpl(DeliverType.SERVER, serverName, playerId);
    }
}
