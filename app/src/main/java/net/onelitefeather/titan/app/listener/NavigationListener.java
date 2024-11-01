package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.titan.common.utils.Items;
import net.onelitefeather.titan.app.helper.NavigationHelper;

import java.util.function.Consumer;

public final class NavigationListener implements Consumer<PlayerUseItemEvent> {

    private final NavigationHelper navigationHelper;

    public NavigationListener(NavigationHelper navigationHelper) {
        this.navigationHelper = navigationHelper;
    }

    @Override
    public void accept(PlayerUseItemEvent event) {
        ItemStack item = event.getItemStack();
        if (item.isSimilar(Items.PLAYER_TELEPORTER)) {
            this.navigationHelper.openNavigator(event.getPlayer());
        }
    }
}
