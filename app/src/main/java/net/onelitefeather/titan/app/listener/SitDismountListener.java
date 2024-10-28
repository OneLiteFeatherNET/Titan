package net.onelitefeather.titan.app.listener;

import net.minestom.server.entity.Player;
import net.onelitefeather.titan.common.event.EntityDismountEvent;
import net.onelitefeather.titan.common.helper.SitHelper;

import java.util.function.Consumer;

public final class SitDismountListener implements Consumer<EntityDismountEvent> {
    @Override
    public void accept(EntityDismountEvent event) {
        if (event.rider() instanceof Player player && SitHelper.isSitting(player)) {
            SitHelper.removePlayerFromArrow(event.getEntity());
        }
    }
}
