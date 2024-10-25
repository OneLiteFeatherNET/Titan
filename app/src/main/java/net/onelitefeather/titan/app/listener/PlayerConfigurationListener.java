package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.common.utils.Cancelable;

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
        Optional.of(this.mapProvider.getActiveLobby()).map(LobbyMap::getSpawn).ifPresent(event.getPlayer()::setRespawnPoint);
        event.getPlayer().getInventory().addInventoryCondition(Cancelable::cancelClick);
    }
}
