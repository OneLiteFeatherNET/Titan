package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.common.utils.Cancelable;

import java.util.Optional;
import java.util.function.Consumer;

public final class PlayerConfigurationListener implements Consumer<AsyncPlayerConfigurationEvent> {

    private final MapProvider mapProvider;

    public PlayerConfigurationListener(MapProvider mapProvider) {
        this.mapProvider = mapProvider;
    }

    @Override
    public void accept(AsyncPlayerConfigurationEvent event) {
        Optional.ofNullable(this.mapProvider).map(MapProvider::getInstance).ifPresent(event::setSpawningInstance);
        Optional.of(this.mapProvider).map(MapProvider::getActiveLobby).map(LobbyMap::getSpawn).ifPresent(event.getPlayer()::setRespawnPoint);
        event.getPlayer().getInventory().addInventoryCondition(Cancelable::cancelClick);
    }
}
