package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.player.PlayerMoveEvent;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.map.LobbyMap;

import java.util.Optional;
import java.util.function.Consumer;

public final class PlayerMoveListener implements Consumer<PlayerMoveEvent> {
    private final AppConfig appConfig;
    private final LobbyMap lobbyMap;

    public PlayerMoveListener(AppConfig appConfig, LobbyMap lobbyMap) {
        this.appConfig = appConfig;
        this.lobbyMap = lobbyMap;
    }

    @Override
    public void accept(PlayerMoveEvent playerMoveEvent) {
        var player = playerMoveEvent.getPlayer();
        if (player.getInstance() == null) return;
        if (player.getPosition().y() < appConfig.minHeightBeforeTeleport()) {
            Optional.ofNullable(this.lobbyMap).map(LobbyMap::getSpawn).ifPresent(player::teleport);
            return;
        }
        if (player.getPosition().y() > appConfig.maxHeightBeforeTeleport()) {
            Optional.ofNullable(this.lobbyMap).map(LobbyMap::getSpawn).ifPresent(player::teleport);
        }
    }
}
