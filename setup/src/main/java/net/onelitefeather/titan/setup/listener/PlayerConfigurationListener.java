package net.onelitefeather.titan.setup.listener;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapProvider;

import java.util.Optional;
import java.util.function.Consumer;

public class PlayerConfigurationListener implements Consumer<AsyncPlayerConfigurationEvent> {

    private final MapProvider mapProvider;

    public PlayerConfigurationListener(MapProvider mapProvider) {
        this.mapProvider = mapProvider;
    }

    @Override
    public void accept(AsyncPlayerConfigurationEvent event) {
        event.setSpawningInstance(this.mapProvider.getInstance());
        event.getPlayer().setGameMode(GameMode.CREATIVE);
        Pos pos = Optional.of(this.mapProvider)
                .map(MapProvider::getActiveLobby)
                .map(LobbyMap::getSpawn)
                .orElse(null);
        if (pos == null) return;
        event.getPlayer().setRespawnPoint(pos);
    }
}
