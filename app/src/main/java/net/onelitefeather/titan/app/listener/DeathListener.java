package net.onelitefeather.titan.app.listener;

import net.kyori.adventure.text.Component;
import net.minestom.server.event.player.PlayerDeathEvent;

import java.util.function.Consumer;

public final class DeathListener implements Consumer<PlayerDeathEvent> {

    @Override
    public void accept(PlayerDeathEvent event) {
        event.setDeathText(Component.empty());
        event.getPlayer().respawn();
    }
}
