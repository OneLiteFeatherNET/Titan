package net.onelitefeather.titan.setup.listener;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.CachedPacket;
import net.minestom.server.network.packet.server.play.UpdateSimulationDistancePacket;
import net.onelitefeather.titan.common.config.AppConfig;

import java.util.Optional;
import java.util.function.Consumer;

public class PlayerSpawnListener implements Consumer<PlayerSpawnEvent> {

    private final CachedPacket simulatedDistancePacket;

    public PlayerSpawnListener(AppConfig appConfig) {
        simulatedDistancePacket = new CachedPacket(new UpdateSimulationDistancePacket(appConfig.simulationDistance()));
    }

    @Override
    public void accept(PlayerSpawnEvent event) {
        event.getPlayer().sendPacket(this.simulatedDistancePacket);
        // Optional.of(event).map(PlayerSpawnEvent::getPlayer).map(Player::getInstance).map(Instance::getWorldSpawnPosition).ifPresent(event.getPlayer()::teleport);
    }
}
