package net.onelitefeather.titan.common.utils;

import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.trait.CancellableEvent;

public final class Cancelable {

    private Cancelable() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static void cancel(CancellableEvent event) {
        event.setCancelled(true);
    }
}
