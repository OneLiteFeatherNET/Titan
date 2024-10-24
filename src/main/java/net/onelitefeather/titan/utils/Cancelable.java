package net.onelitefeather.titan.utils;

import net.minestom.server.event.trait.CancellableEvent;

public final class Cancelable {

    private Cancelable() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static void cancel(CancellableEvent event) {
        event.setCancelled(true);
    }

}
