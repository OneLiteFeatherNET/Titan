package net.onelitefeather.titan.setup.listener;

import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.packet.server.CachedPacket;
import net.minestom.server.network.packet.server.play.UpdateSimulationDistancePacket;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapProvider;

import java.util.Optional;
import java.util.function.Consumer;

public class PlayerSpawnListener implements Consumer<PlayerSpawnEvent> {

    private final CachedPacket simulatedDistancePacket;
    private final MapProvider mapProvider;

    public PlayerSpawnListener(AppConfig appConfig, MapProvider mapProvider) {
        this.simulatedDistancePacket = new CachedPacket(new UpdateSimulationDistancePacket(appConfig.simulationDistance()));
        this.mapProvider = mapProvider;
    }

    @Override
    public void accept(PlayerSpawnEvent event) {
        event.getPlayer().sendPacket(this.simulatedDistancePacket);
        Optional.of(this.mapProvider)
                .map(MapProvider::getActiveLobby)
                .map(LobbyMap::getSpawn)
                .ifPresent(event.getPlayer()::teleport);
    }
}
