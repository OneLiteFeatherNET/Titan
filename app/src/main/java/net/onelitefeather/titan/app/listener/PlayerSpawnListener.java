package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.packet.server.CachedPacket;
import net.minestom.server.network.packet.server.play.UpdateSimulationDistancePacket;
import net.onelitefeather.titan.app.helper.NavigationHelper;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.map.LobbyMap;

import java.util.Optional;
import java.util.function.Consumer;

public final class PlayerSpawnListener implements Consumer<PlayerSpawnEvent> {

    private final CachedPacket simulatedDistancePacket;
    private final LobbyMap lobbyMap;
    private final NavigationHelper navigationHelper;

    public PlayerSpawnListener(AppConfig appConfig, LobbyMap lobbyMap, NavigationHelper navigationHelper) {
        simulatedDistancePacket = new CachedPacket(new UpdateSimulationDistancePacket(appConfig.simulationDistance()));
        this.lobbyMap = lobbyMap;
        this.navigationHelper = navigationHelper;
    }

    @Override
    public void accept(PlayerSpawnEvent event) {
        event.getPlayer().sendPacket(this.simulatedDistancePacket);
        Optional.ofNullable(this.lobbyMap).map(LobbyMap::getSpawn).ifPresent(event.getPlayer()::teleport);
        this.navigationHelper.setItems(event.getPlayer());
    }
}
