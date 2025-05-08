package net.onelitefeather.titan.app.listener;

import net.kyori.adventure.key.Key;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.helper.SitHelper;

import java.util.List;
import java.util.function.Consumer;

public final class SitListener implements Consumer<PlayerBlockInteractEvent> {

    private final AppConfig appConfig;
    private final List<Key> allowedBlocks;

    public SitListener(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.allowedBlocks = appConfig.allowedSitBlocks();
    }

    @Override
    public void accept(PlayerBlockInteractEvent event) {
        if (this.allowedBlocks.stream().anyMatch(block -> event.getBlock().key().equals(block))) {
            SitHelper.sitPlayer(event.getPlayer(), event.getBlockPosition(), this.appConfig);
        }
    }
}
