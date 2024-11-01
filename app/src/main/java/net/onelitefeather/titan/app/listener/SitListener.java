package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.utils.NamespaceID;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.helper.SitHelper;

import java.util.List;
import java.util.function.Consumer;

public final class SitListener implements Consumer<PlayerBlockInteractEvent> {

    private final AppConfig appConfig;
    private final List<NamespaceID> allowedBlocks;

    public SitListener(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.allowedBlocks = appConfig.allowedSitBlocks();
    }

    @Override
    public void accept(PlayerBlockInteractEvent event) {
        if (this.allowedBlocks.stream().anyMatch(block -> event.getBlock().namespace().equals(block))) {
            SitHelper.sitPlayer(event.getPlayer(), event.getBlockPosition(), this.appConfig);
        }
    }
}
