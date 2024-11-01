package net.onelitefeather.deliver;

import java.util.UUID;

final class GameServerBuilderImpl implements DeliverComponent.GameServerBuilder {

    private String gameServerId;
    private UUID playerId;

    @Override
    public DeliverComponent.GameServerBuilder gameServerId(String gameServerId) {
        this.gameServerId = gameServerId;
        return this;
    }

    @Override
    public DeliverComponent.GameServerBuilder playerId(UUID playerId) {
        this.playerId = playerId;
        return this;
    }

    @Override
    public DeliverComponent build() {
        if (gameServerId == null) {
            throw new IllegalArgumentException("gameServerId cannot be null");
        }
        if (playerId == null) {
            throw new IllegalArgumentException("playerId cannot be null");
        }
        return new GameServerDeliverComponentImpl(DeliverType.GAME_SERVER, gameServerId, playerId);
    }
}
