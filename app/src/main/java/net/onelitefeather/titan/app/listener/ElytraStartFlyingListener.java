package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.utils.Items;

import java.util.function.Consumer;

public final class ElytraStartFlyingListener implements Consumer<PlayerStartFlyingWithElytraEvent> {

    private final AppConfig appConfig;

    public ElytraStartFlyingListener(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void accept(PlayerStartFlyingWithElytraEvent event) {
        event.getPlayer().getInventory().setItemStack(this.appConfig.fireworkBoostSlot(), Items.PLAYER_FIREWORK);
    }
}
