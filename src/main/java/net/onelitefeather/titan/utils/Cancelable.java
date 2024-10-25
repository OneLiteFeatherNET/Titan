package net.onelitefeather.titan.utils;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.inventory.click.InventoryClickResult;
import net.minestom.server.inventory.condition.InventoryConditionResult;

public final class Cancelable {

    private Cancelable() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static void cancel(CancellableEvent event) {
        event.setCancelled(true);
    }

    public static void cancelClick(Player player, int slot, ClickType type, InventoryConditionResult inventoryConditionResult) {
        inventoryConditionResult.setCancel(true);
    }

}
