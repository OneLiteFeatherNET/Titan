package net.onelitefeather.titan.function;

import net.kyori.adventure.text.Component;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;

public final class DeathFunction {


    public DeathFunction(EventNode<Event> node) {
        node.addListener(PlayerDeathEvent.class, this::onDeath);
    }

    private void onDeath(final PlayerDeathEvent event) {
        event.setDeathText(Component.empty());
        event.getPlayer().respawn();
    }

    public static DeathFunction instance(EventNode<Event> node) {
        return new DeathFunction(node);
    }
}
