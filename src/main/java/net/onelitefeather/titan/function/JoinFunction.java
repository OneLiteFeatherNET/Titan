package net.onelitefeather.titan.function;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.packet.server.CachedPacket;
import net.minestom.server.network.packet.server.play.UpdateSimulationDistancePacket;
import net.onelitefeather.titan.utils.Cancelable;

public final class JoinFunction {

    private static final CachedPacket SIMULATED_DISTANCE_PACKET = new CachedPacket(new UpdateSimulationDistancePacket(2));
    private final Pos spawn;
    private final NavigationFunction navigationFunction;

    private void playerLoginEvent(AsyncPlayerConfigurationEvent event) {
        event.setSpawningInstance(WorldFunction.lobbyInstance());
        event.getPlayer().setRespawnPoint(spawn);
        event.getPlayer().getInventory().addInventoryCondition(Cancelable::cancelClick);
    }

    private void playerJoinEvent(PlayerSpawnEvent event) {
        event.getPlayer().sendPacket(SIMULATED_DISTANCE_PACKET);
        event.getPlayer().teleport(spawn);
        this.navigationFunction.setItems(event.getPlayer());
    }

    private JoinFunction(EventNode<Event> node, Pos spawn, NavigationFunction navigationFunction) {
        this.spawn = spawn;
        this.navigationFunction = navigationFunction;
        node.addListener(AsyncPlayerConfigurationEvent.class, this::playerLoginEvent);
        node.addListener(PlayerSpawnEvent.class, this::playerJoinEvent);
    }

    public static JoinFunction instance(EventNode<Event> node, Pos spawn, NavigationFunction navigationFunction) {
        return new JoinFunction(node, spawn, navigationFunction);
    }

}
