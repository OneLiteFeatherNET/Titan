package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent;
import net.minestom.server.item.ItemStack;

import java.util.function.Consumer;

public final class ElytraStopFlyingListener implements Consumer<PlayerStopFlyingWithElytraEvent> {

    @Override
    public void accept(PlayerStopFlyingWithElytraEvent event) {
        event.getPlayer().getInventory().setItemInOffHand(ItemStack.AIR);
        event.getPlayer().getInventory().update();
    }
}
