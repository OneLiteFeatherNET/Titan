package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.player.PlayerRespawnEvent;
import net.onelitefeather.titan.app.helper.NavigationHelper;

import java.util.function.Consumer;

public final class RespawnListener implements Consumer<PlayerRespawnEvent> {

    private final NavigationHelper navigationHelper;

    public RespawnListener(NavigationHelper navigationHelper) {
        this.navigationHelper = navigationHelper;
    }

    @Override
    public void accept(PlayerRespawnEvent event) {
        this.navigationHelper.setItems(event.getPlayer());
    }
}
