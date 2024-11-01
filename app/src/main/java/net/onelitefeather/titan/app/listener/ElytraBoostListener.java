package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.player.PlayerUseItemEvent;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.utils.Items;

import java.util.function.Consumer;

public final class ElytraBoostListener implements Consumer<PlayerUseItemEvent> {

    private final AppConfig appConfig;

    public ElytraBoostListener(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void accept(PlayerUseItemEvent event) {
        var itemStack = event.getItemStack();
        if (itemStack.isSimilar(Items.PLAYER_FIREWORK) && event.getPlayer().isFlyingWithElytra()) {
            event.getPlayer().setVelocity(event.getPlayer().getVelocity().normalize().add(event.getPlayer().getPosition().direction().mul(this.appConfig.elytraBoostMultiplier())));
        }
    }
}
