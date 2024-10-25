package net.onelitefeather.titan.function;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerRespawnEvent;

public class RespawnFunction {

    private final NavigationFunction navigationFunction;

    private void respawnListener(PlayerRespawnEvent event) {
        this.navigationFunction.setItems(event.getPlayer());
    }

    private RespawnFunction(EventNode<Event> node, NavigationFunction navigationFunction) {
        this.navigationFunction = navigationFunction;
        node.addListener(PlayerRespawnEvent.class, this::respawnListener);
    }

    public static RespawnFunction instance(EventNode<Event> node, NavigationFunction navigationFunction) {
        return new RespawnFunction(node, navigationFunction);
    }

}
