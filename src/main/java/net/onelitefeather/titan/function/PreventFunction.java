package net.onelitefeather.titan.function;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.onelitefeather.titan.utils.Cancelable;

public final class PreventFunction {

    private PreventFunction(EventNode<Event> node) {
        node.addListener(PickupItemEvent.class, Cancelable::cancel);
        node.addListener(PlayerBlockBreakEvent.class, Cancelable::cancel);
        node.addListener(PlayerBlockPlaceEvent.class, Cancelable::cancel);
        node.addListener(PlayerSwapItemEvent.class, Cancelable::cancel);
        node.addListener(ItemDropEvent.class, Cancelable::cancel);
    }

    public static PreventFunction instance(EventNode<Event> node) {
        return new PreventFunction(node);
    }
}
