package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.titan.common.config.AppConfig;

import java.util.function.Consumer;

public final class ElytraStopFlyingListener implements Consumer<PlayerStartFlyingWithElytraEvent> {

    private final AppConfig appConfig;

    public ElytraStopFlyingListener(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void accept(PlayerStartFlyingWithElytraEvent event) {
        event.getPlayer().getInventory().setItemStack(this.appConfig.fireworkBoostSlot(), ItemStack.AIR);
    }
}
