package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.titan.common.helper.SitHelper;

import java.util.function.Consumer;

public final class SitDisconnectListener implements Consumer<PlayerDisconnectEvent> {
    @Override
    public void accept(PlayerDisconnectEvent event) {
        SitHelper.removePlayer(event.getPlayer());
    }
}
