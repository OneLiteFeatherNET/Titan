package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.onelitefeather.titan.common.utils.Items;

import java.util.function.Consumer;

public final class ElytraStartFlyingListener implements Consumer<PlayerStartFlyingWithElytraEvent> {
    @Override
    public void accept(PlayerStartFlyingWithElytraEvent event) {
        event.getPlayer().getInventory().setItemInOffHand(Items.PLAYER_FIREWORK);
        event.getPlayer().getInventory().update();
    }
}
